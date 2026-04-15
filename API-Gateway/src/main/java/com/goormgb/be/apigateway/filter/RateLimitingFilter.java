package com.goormgb.be.apigateway.filter;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 애플리케이션 레벨 Rate Limiting — 엔드포인트 민감도별 차등 한도.
 *
 * Istio EnvoyFilter (IP+path) 가 1차 방어선이라면 본 필터는 2차 방어선.
 * 경로별 카테고리를 분리하여 브루트포스/크리덴셜 스터핑/예매 남용에 대응.
 *
 * <h3>카테고리</h3>
 * <ul>
 *   <li>LOGIN       — /auth/**(/login$)       IP당 5/분 (Istio 의 /auth/ 10/s 와 중첩)</li>
 *   <li>SIGNUP      — /auth/signup, /auth/loadtest/signup  IP당 3/분</li>
 *   <li>REFRESH     — /auth/token/refresh     IP당 30/분 (토큰 갱신은 정상 트래픽 많음)</li>
 *   <li>PASSWORD    — /auth/password/**       IP당 3/분</li>
 *   <li>PRECHECK    — /ai/precheck/**         IP당 20/분 (봇탐지 엔드포인트 자체 남용 방지)</li>
 *   <li>QUEUE_ENTER — /queue/matches/*/enter  userId(있으면) 또는 IP 기준 20/분 (Spring Cloud Gateway RequestRateLimiter 와 중첩)</li>
 *   <li>PAYMENT     — /payment/**             IP당 10/분</li>
 *   <li>SEAT_HOLD   — /seat/**/hold           IP당 30/분</li>
 *   <li>DEFAULT     — 그 외                   IP당 200/분 (기존 100 → 상향: 대부분 정상 API 트래픽 수용)</li>
 * </ul>
 *
 * <h3>키 구성</h3>
 * 기본적으로 IP 기반. userId 헤더가 상류에서 주입됐다면 (X-User-Id) userId 기반 키도 병행.
 * <p>
 * 실행 순서: -3 (JwtAuthenticationFilter -1 보다 먼저 → 인증 부하 감소).
 * 단, X-User-Id 는 JWT 파싱 후에 주입되므로 본 필터 단독으로는 userId 키 불가.
 * 재요청 경로에서는 User-Agent 기반 로그인 실패 경우만 IP 키로 처리.
 * <p>
 * Redis 장애 시 fail-open.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RateLimitingFilter implements GlobalFilter, Ordered {

	private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
	private static final Duration WINDOW_TTL = Duration.ofSeconds(60);

	/** 카테고리 정의. 순서대로 첫 매칭 사용. */
	private enum Category {
		LOGIN     ("rate_limit:login:",        5,   Pattern.compile("^/auth/.+/login/?$"),           Pattern.compile("POST")),
		SIGNUP    ("rate_limit:signup:",       3,   Pattern.compile("^/auth/(loadtest/)?signup/?$"), Pattern.compile("POST")),
		PASSWORD  ("rate_limit:password:",     3,   Pattern.compile("^/auth/password(/.*)?$"),       Pattern.compile("POST|PUT|PATCH")),
		REFRESH   ("rate_limit:refresh:",      30,  Pattern.compile("^/auth/token/refresh/?$"),      Pattern.compile("POST")),
		PRECHECK  ("rate_limit:precheck:",     20,  Pattern.compile("^/ai/precheck(/.*)?$"),         Pattern.compile("POST")),
		QUEUE_ENTER("rate_limit:queue_enter:", 20,  Pattern.compile("^/queue/matches/[^/]+/enter/?$"), Pattern.compile("POST")),
		PAYMENT   ("rate_limit:payment:",      10,  Pattern.compile("^/payment(/.*)?$"),             Pattern.compile("POST|PUT|PATCH")),
		SEAT_HOLD ("rate_limit:seat_hold:",    30,  Pattern.compile("^/seat/.+/hold/?$"),            Pattern.compile("POST|PUT|PATCH|DELETE")),
		DEFAULT   ("rate_limit:api:",          200, null, null);

		final String keyPrefix;
		final long limitPerMinute;
		final Pattern pathPattern;
		final Pattern methodPattern;

		Category(String keyPrefix, long limit, Pattern path, Pattern method) {
			this.keyPrefix = keyPrefix;
			this.limitPerMinute = limit;
			this.pathPattern = path;
			this.methodPattern = method;
		}

		static Category resolve(HttpMethod method, String path) {
			if (method == null) return DEFAULT;
			String m = method.name();
			for (Category c : values()) {
				if (c == DEFAULT) continue;
				if (c.pathPattern.matcher(path).matches()
						&& c.methodPattern.matcher(m).matches()) {
					return c;
				}
			}
			return DEFAULT;
		}
	}

	private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE_SCRIPT = buildIncrWithExpireScript();

	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String clientIp = resolveClientIp(request);

		Category category = Category.resolve(request.getMethod(), request.getPath().value());
		String rateLimitKey = category.keyPrefix + clientIp;
		String ttlSeconds = String.valueOf(WINDOW_TTL.getSeconds());

		return reactiveRedisTemplate.execute(
				INCR_WITH_EXPIRE_SCRIPT,
				List.of(rateLimitKey),
				ttlSeconds
			)
			.next()
			.flatMap(currentCount -> applyLimit(exchange, chain, category, rateLimitKey, currentCount, clientIp))
			.onErrorResume(ex -> {
				// Redis 장애 시 가용성 우선: fail-open
				log.error("Rate limit check failed. path={}, clientIp={}, category={}, error={}",
					request.getPath().value(), clientIp, category, ex.getMessage(), ex);
				return chain.filter(exchange);
			});
	}

	private Mono<Void> applyLimit(
		ServerWebExchange exchange,
		GatewayFilterChain chain,
		Category category,
		String rateLimitKey,
		Long currentCount,
		String clientIp
	) {
		if (currentCount != null && currentCount > category.limitPerMinute) {
			log.warn("Rate limit exceeded. category={}, key={}, clientIp={}, count={}, limit={}",
				category, rateLimitKey, clientIp, currentCount, category.limitPerMinute);
			exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
			exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, "60");
			exchange.getResponse().getHeaders().set("X-RateLimit-Category", category.name());
			exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(category.limitPerMinute));
			return exchange.getResponse().setComplete();
		}
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		// JwtAuthenticationFilter(-1) 보다 먼저 적용되어 다운스트림/인증 부하를 줄인다.
		return -3;
	}

	private String resolveClientIp(ServerHttpRequest request) {
		String xForwardedFor = request.getHeaders().getFirst(HEADER_X_FORWARDED_FOR);
		if (StringUtils.hasText(xForwardedFor)) {
			String firstIp = xForwardedFor.split(",")[0].trim();
			if (StringUtils.hasText(firstIp)) {
				return firstIp;
			}
		}

		InetSocketAddress remoteAddress = request.getRemoteAddress();
		if (remoteAddress != null) {
			if (remoteAddress.getAddress() != null && StringUtils.hasText(remoteAddress.getAddress().getHostAddress())) {
				return remoteAddress.getAddress().getHostAddress();
			}
			if (StringUtils.hasText(remoteAddress.getHostString())) {
				return remoteAddress.getHostString();
			}
		}

		return "unknown";
	}

	private static DefaultRedisScript<Long> buildIncrWithExpireScript() {
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
}
