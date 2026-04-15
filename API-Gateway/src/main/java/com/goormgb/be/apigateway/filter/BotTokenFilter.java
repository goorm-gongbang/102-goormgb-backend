package com.goormgb.be.apigateway.filter;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * X-Bot-Token 2-way 서버 검증 필터.
 *
 * 토큰 포맷: {@code <base64url(payload_json)>.<hex(HMAC-SHA256(payload, SECRET))>}
 * payload_json = {@code {v, ts, nonce, fp, meta}}
 *
 * 검증 단계:
 * <ol>
 *   <li>헤더 존재 여부</li>
 *   <li>payload/signature 분리</li>
 *   <li>HMAC 재계산 + MessageDigest.isEqual 비교</li>
 *   <li>ts 가 현재 ±5분 이내</li>
 *   <li>nonce 가 Redis 에 5분 이내 재등장하지 않았는지 (있어도 첫 관찰 기준 허용 — 캐싱된 토큰 재사용은 정상)</li>
 *   <li>fp 별 5분 내 요청량 {@code BOT_FP_MAX_REQS_PER_5MIN} 이하</li>
 * </ol>
 *
 * 보호 경로만 적용 — 전체 요청에 걸면 공개 조회 API 성능 저하.
 * 이 필터는 RateLimitingFilter(-3) 이후, JwtAuthenticationFilter(-1) 이전에 실행.
 *
 * 실패 시 403 Forbidden. Redis 장애 시 가용성 우선 fail-open.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class BotTokenFilter implements GlobalFilter, Ordered {

	private static final String HEADER_BOT_TOKEN = "X-Bot-Token";
	private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

	/** 허용 시계 드리프트 (±5분) */
	private static final long MAX_CLOCK_SKEW_SEC = 300L;

	/** fp 별 5분 내 최대 요청 수 */
	private static final long BOT_FP_MAX_REQS_PER_5MIN = 600L;

	private static final Duration FP_WINDOW = Duration.ofMinutes(5);
	private static final Duration NONCE_TTL = Duration.ofMinutes(10);

	private static final String NONCE_KEY_PREFIX = "bot:nonce:";
	private static final String FP_COUNT_KEY_PREFIX = "bot:fp:";

	/** 보호 경로 (HTTP 메서드 + 경로 prefix). 여기에 매칭되는 요청만 검증. */
	private static final Set<HttpMethod> MUTATING = Set.of(
			HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

	private static final List<ProtectedEntry> PROTECTED = List.of(
			new ProtectedEntry(MUTATING, "/auth/kakao/login"),
			new ProtectedEntry(MUTATING, "/auth/dev/auth"),
			new ProtectedEntry(MUTATING, "/auth/token/refresh"),
			new ProtectedEntry(MUTATING, "/ai/precheck"),
			new ProtectedEntry(MUTATING, "/queue"),
			new ProtectedEntry(MUTATING, "/seat"),
			new ProtectedEntry(MUTATING, "/order"),
			new ProtectedEntry(MUTATING, "/payment")
	);

	private record ProtectedEntry(Set<HttpMethod> methods, String pathPrefix) {}

	private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE = buildIncrScript();

	private final ReactiveRedisTemplate<String, String> redis;
	private final ObjectMapper objectMapper;

	@Value("${bot.token.secret:playball-xbot-v1}")
	private String secret;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getPath().value();
		HttpMethod method = request.getMethod();

		if (!isProtected(method, path)) {
			return chain.filter(exchange);
		}

		String token = request.getHeaders().getFirst(HEADER_BOT_TOKEN);
		if (!StringUtils.hasText(token)) {
			return reject(exchange, "missing_token", path);
		}

		TokenPayload payload;
		try {
			payload = parseAndVerify(token);
		} catch (BotTokenException e) {
			return reject(exchange, e.reason, path);
		}

		// 시계 드리프트 검증
		long now = System.currentTimeMillis() / 1000L;
		if (Math.abs(now - payload.ts) > MAX_CLOCK_SKEW_SEC) {
			return reject(exchange, "token_expired", path);
		}

		String fpCountKey = FP_COUNT_KEY_PREFIX + payload.fp;
		String nonceKey = NONCE_KEY_PREFIX + payload.nonce;

		// fp 요청량 체크 + nonce 최초 관찰 기록. 재사용은 캐싱 토큰이라 허용.
		return redis.execute(INCR_WITH_EXPIRE, List.of(fpCountKey),
						String.valueOf(FP_WINDOW.getSeconds()))
				.next()
				.flatMap(count -> {
					if (count != null && count > BOT_FP_MAX_REQS_PER_5MIN) {
						return reject(exchange, "fp_flood", path);
					}
					return redis.opsForValue()
							.setIfAbsent(nonceKey, "1", NONCE_TTL)
							.thenReturn(count);
				})
				.flatMap(c -> chain.filter(exchange))
				.onErrorResume(ex -> {
					// Redis 장애: 가용성 우선 fail-open
					log.error("BotToken verify error (fail-open). path={}, error={}",
							path, ex.getMessage(), ex);
					return chain.filter(exchange);
				});
	}

	@Override
	public int getOrder() {
		// RateLimit(-3) 이후, JWT(-1) 이전
		return -2;
	}

	private boolean isProtected(HttpMethod method, String path) {
		if (method == null) return false;
		for (ProtectedEntry e : PROTECTED) {
			if (!e.methods().contains(method)) continue;
			if (path.equals(e.pathPrefix()) || path.startsWith(e.pathPrefix() + "/")) {
				return true;
			}
		}
		return false;
	}

	private TokenPayload parseAndVerify(String token) throws BotTokenException {
		int dot = token.indexOf('.');
		if (dot <= 0 || dot == token.length() - 1) {
			throw new BotTokenException("malformed_token");
		}
		String payloadB64 = token.substring(0, dot);
		String sig = token.substring(dot + 1);

		String expectedSig;
		String payloadJson;
		try {
			expectedSig = hmacSha256Hex(payloadB64, secret);
			byte[] decoded = Base64.getUrlDecoder().decode(payloadB64);
			payloadJson = new String(decoded, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new BotTokenException("decode_fail");
		}

		if (!MessageDigest.isEqual(sig.getBytes(StandardCharsets.UTF_8),
				expectedSig.getBytes(StandardCharsets.UTF_8))) {
			throw new BotTokenException("bad_signature");
		}

		try {
			JsonNode n = objectMapper.readTree(payloadJson);
			return new TokenPayload(
					n.path("v").asText(""),
					n.path("ts").asLong(0L),
					n.path("nonce").asText(""),
					n.path("fp").asText(""),
					n.path("meta").asText("")
			);
		} catch (Exception e) {
			throw new BotTokenException("bad_payload");
		}
	}

	private Mono<Void> reject(ServerWebExchange exchange, String reason, String path) {
		String clientIp = resolveClientIp(exchange.getRequest());
		log.warn("BotToken rejected. reason={}, path={}, clientIp={}", reason, path, clientIp);
		exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
		exchange.getResponse().getHeaders().add("X-Bot-Reject-Reason", reason);
		return exchange.getResponse().setComplete();
	}

	private String resolveClientIp(ServerHttpRequest request) {
		String xff = request.getHeaders().getFirst(HEADER_X_FORWARDED_FOR);
		if (StringUtils.hasText(xff)) {
			String firstIp = xff.split(",")[0].trim();
			if (StringUtils.hasText(firstIp)) return firstIp;
		}
		InetSocketAddress addr = request.getRemoteAddress();
		if (addr != null && addr.getAddress() != null) {
			return addr.getAddress().getHostAddress();
		}
		return "unknown";
	}

	private static String hmacSha256Hex(String message, String secret) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder(digest.length * 2);
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static DefaultRedisScript<Long> buildIncrScript() {
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setResultType(Long.class);
		script.setScriptText("""
				local current = redis.call('INCR', KEYS[1])
				if current == 1 then
				  redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
				end
				return current
				""");
		return script;
	}

	private record TokenPayload(String v, long ts, String nonce, String fp, String meta) {}

	private static final class BotTokenException extends Exception {
		final String reason;

		BotTokenException(String reason) {
			super(reason);
			this.reason = reason;
		}
	}
}
