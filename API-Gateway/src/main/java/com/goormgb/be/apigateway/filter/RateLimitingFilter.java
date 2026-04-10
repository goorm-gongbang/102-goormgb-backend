package com.goormgb.be.apigateway.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Redis 기반 IP별 Rate Limiting 필터.
 * prod 환경에서만 활성화된다.
 *
 * <ul>
 *   <li>로그인 관련 경로(/auth/kakao, /auth/loadtest/login): IP당 분당 10회</li>
 *   <li>일반 API: IP당 분당 100회</li>
 *   <li>Redis 장애 시 fail-open (제한 없이 통과)</li>
 *   <li>INCR + EXPIRE를 Lua 스크립트로 원자적 실행</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RateLimitingFilter implements GlobalFilter, Ordered {

	private static final int LOGIN_LIMIT_PER_MINUTE = 10;
	private static final int GENERAL_LIMIT_PER_MINUTE = 100;
	private static final long WINDOW_SECONDS = 60;
	private static final String RATE_LIMIT_PREFIX = "rate_limit:";

	/**
	 * Lua 스크립트: INCR과 EXPIRE를 원자적으로 실행한다.
	 * 키가 새로 생성된 경우(count == 1)에만 TTL을 설정하며,
	 * INCR 후 EXPIRE 실패로 TTL 없는 키가 남아 영구 차단되는 문제를 방지한다.
	 */
	private static final RedisScript<Long> INCREMENT_AND_EXPIRE_SCRIPT = RedisScript.of(
		"local count = redis.call('INCR', KEYS[1])\n" +
		"if count == 1 then\n" +
		"  redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
		"end\n" +
		"return count",
		Long.class
	);

	private final ReactiveStringRedisTemplate redisTemplate;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		String clientIp = resolveClientIp(exchange);

		boolean isLoginPath = path.startsWith("/auth/kakao")
			|| path.startsWith("/auth/loadtest/login")
			|| path.startsWith("/auth/token/refresh");

		int limit = isLoginPath ? LOGIN_LIMIT_PER_MINUTE : GENERAL_LIMIT_PER_MINUTE;
		String category = isLoginPath ? "login" : "general";
		String redisKey = RATE_LIMIT_PREFIX + category + ":" + clientIp;

		return redisTemplate.execute(
				INCREMENT_AND_EXPIRE_SCRIPT,
				List.of(redisKey),
				List.of(String.valueOf(WINDOW_SECONDS))
			)
			.next()
			.flatMap(count -> {
				if (count > limit) {
					log.warn("[RateLimit] IP={} path={} category={} count={} → 429 차단",
						clientIp, path, category, count);
					exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
					return exchange.getResponse().setComplete();
				}
				return chain.filter(exchange);
			})
			// Redis 장애 시 fail-open: 제한 없이 통과시켜 서비스 가용성 유지
			.onErrorResume(ex -> {
				log.error("[RateLimit] Redis 장애 — fail-open 처리. error={}", ex.getMessage());
				return chain.filter(exchange);
			});
	}

	@Override
	public int getOrder() {
		return -2;
	}

	/**
	 * 클라이언트 IP를 추출한다.
	 * prod 환경에서는 Cloudflare → ALB → Gateway 순서로 요청이 들어오며,
	 * Cloudflare가 X-Forwarded-For 헤더를 신뢰할 수 있는 값으로 덮어씌우므로
	 * 클라이언트가 직접 헤더를 조작하여 Rate Limiting을 우회할 수 없다.
	 */
	private String resolveClientIp(ServerWebExchange exchange) {
		String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			return xff.split(",")[0].trim();
		}
		if (exchange.getRequest().getRemoteAddress() != null) {
			return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
		}
		return "unknown";
	}
}
