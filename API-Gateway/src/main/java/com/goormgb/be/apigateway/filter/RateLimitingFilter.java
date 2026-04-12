package com.goormgb.be.apigateway.filter;

import java.time.Duration;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * IP 기반 API Rate Limiting 필터.
 *
 * <p>Redis INCR + TTL 방식으로 IP별 요청 횟수를 제한한다.
 * 로그인 경로는 분당 10회, 일반 경로는 분당 100회로 제한하며
 * 초과 시 429 Too Many Requests를 반환한다.</p>
 *
 * <p>prod 프로필에서만 활성화되어 staging 부하테스트에 영향을 주지 않는다.</p>
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RateLimitingFilter implements GlobalFilter, Ordered {

	private static final int LOGIN_LIMIT = 10;
	private static final int GENERAL_LIMIT = 100;
	private static final Duration WINDOW = Duration.ofMinutes(1);

	private static final String LOGIN_KEY_PREFIX = "rate_limit:login:";
	private static final String GENERAL_KEY_PREFIX = "rate_limit:general:";

	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String clientIp = resolveClientIp(request);
		String path = request.getPath().value();

		boolean isLoginPath = path.startsWith("/auth/kakao") || path.startsWith("/dev/auth/login");

		String key;
		int limit;
		if (isLoginPath) {
			key = LOGIN_KEY_PREFIX + clientIp;
			limit = LOGIN_LIMIT;
		} else {
			key = GENERAL_KEY_PREFIX + clientIp;
			limit = GENERAL_LIMIT;
		}

		return reactiveRedisTemplate.opsForValue().increment(key)
				.flatMap(count -> {
					if (count == 1) {
						return reactiveRedisTemplate.expire(key, WINDOW)
								.thenReturn(count);
					}
					return Mono.just(count);
				})
				.flatMap(count -> {
					if (count > limit) {
						log.warn("[RateLimit] IP={} 요청 제한 초과 — path={}, count={}, limit={}",
								clientIp, path, count, limit);
						exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
						return exchange.getResponse().setComplete();
					}
					return chain.filter(exchange);
				});
	}

	private String resolveClientIp(ServerHttpRequest request) {
		String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isBlank()) {
			return xForwardedFor.split(",")[0].trim();
		}
		if (request.getRemoteAddress() != null) {
			return request.getRemoteAddress().getAddress().getHostAddress();
		}
		return "unknown";
	}

	@Override
	public int getOrder() {
		return -2;
	}
}
