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

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RateLimitingFilter implements GlobalFilter, Ordered {

	private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
	private static final Pattern LOGIN_PATH_PATTERN = Pattern.compile("^/auth/.+/login/?$");

	private static final String LOGIN_KEY_PREFIX = "rate_limit:login:";
	private static final String API_KEY_PREFIX = "rate_limit:api:";
	private static final Duration WINDOW_TTL = Duration.ofSeconds(60);
	private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE_SCRIPT = buildIncrWithExpireScript();

	private static final long LOGIN_LIMIT_PER_MINUTE = 10L;
	private static final long API_LIMIT_PER_MINUTE = 100L;

	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String clientIp = resolveClientIp(request);
		boolean loginRequest = isLoginRequest(request);

		String keyPrefix = loginRequest ? LOGIN_KEY_PREFIX : API_KEY_PREFIX;
		long limit = loginRequest ? LOGIN_LIMIT_PER_MINUTE : API_LIMIT_PER_MINUTE;
		String rateLimitKey = keyPrefix + clientIp;
		String ttlSeconds = String.valueOf(WINDOW_TTL.getSeconds());

		return reactiveRedisTemplate.execute(
				INCR_WITH_EXPIRE_SCRIPT,
				List.of(rateLimitKey),
				ttlSeconds
			)
			.next()
			.flatMap(currentCount -> applyLimit(exchange, chain, rateLimitKey, currentCount, limit, clientIp))
			.onErrorResume(ex -> {
				// Redis 장애 시 가용성 우선: fail-open (요청 통과)
				log.error("Rate limit check failed. path={}, clientIp={}, error={}",
					request.getPath().value(), clientIp, ex.getMessage(), ex);
				return chain.filter(exchange);
			});
	}

	private Mono<Void> applyLimit(
		ServerWebExchange exchange,
		GatewayFilterChain chain,
		String rateLimitKey,
		Long currentCount,
		long limit,
		String clientIp
	) {
		if (currentCount != null && currentCount > limit) {
			log.warn("Rate limit exceeded. key={}, clientIp={}, count={}, limit={}",
				rateLimitKey, clientIp, currentCount, limit);
			exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
			exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, "60");
			return exchange.getResponse().setComplete();
		}
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		// JwtAuthenticationFilter(-1)보다 먼저 적용되어 다운스트림/인증 부하를 줄인다.
		return -3;
	}

	private boolean isLoginRequest(ServerHttpRequest request) {
		return HttpMethod.POST.equals(request.getMethod())
			&& LOGIN_PATH_PATTERN.matcher(request.getPath().value()).matches();
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
