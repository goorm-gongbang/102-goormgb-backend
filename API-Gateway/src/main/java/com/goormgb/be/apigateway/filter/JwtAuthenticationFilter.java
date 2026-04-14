package com.goormgb.be.apigateway.filter;

import java.util.List;
import java.util.Set;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.goormgb.be.apigateway.jwt.enums.TokenType;
import com.goormgb.be.apigateway.jwt.provider.JwtTokenProvider;
import com.goormgb.be.apigateway.jwt.repository.AccessTokenBlacklistRepository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String HEADER_USER_ID = "X-User-Id";
	private static final String HEADER_USER_ROLE = "X-User-Role";
	private static final String HEADER_SESSION_ID = "X-Session-Id";
	private static final String HEADER_TOKEN_JTI = "X-Token-Jti";

	// 인증 없이 통과시킬 (HTTP 메서드 + 경로 prefix) 쌍 목록
	private static final Set<HttpMethod> GET_ONLY = Set.of(HttpMethod.GET);
	private static final Set<HttpMethod> POST_ONLY = Set.of(HttpMethod.POST);

	private static final List<WhitelistEntry> WHITELIST = List.of(
			// 인증 엔드포인트 — 경로별 메서드 분리
			new WhitelistEntry(GET_ONLY, "/auth/kakao/login-url"),
			new WhitelistEntry(POST_ONLY, "/auth/kakao/login"),
			new WhitelistEntry(POST_ONLY, "/auth/token/refresh"),
			new WhitelistEntry(POST_ONLY, "/auth/dev/auth"),
			new WhitelistEntry(POST_ONLY, "/auth/loadtest"),

			// Swagger / OpenAPI 문서 — GET 전용
			new WhitelistEntry(GET_ONLY, "/swagger-ui"),
			new WhitelistEntry(GET_ONLY, "/v3/api-docs"),
			new WhitelistEntry(GET_ONLY, "/auth/v3/api-docs"),
			new WhitelistEntry(GET_ONLY, "/queue/v3/api-docs"),
			new WhitelistEntry(GET_ONLY, "/seat/v3/api-docs"),
			new WhitelistEntry(GET_ONLY, "/order/v3/api-docs"),
			new WhitelistEntry(GET_ONLY, "/recommendation/v3/api-docs"),

			// Actuator 공개 엔드포인트 — GET 전용
			new WhitelistEntry(GET_ONLY, "/actuator/health"),
			new WhitelistEntry(GET_ONLY, "/actuator/prometheus"),
			new WhitelistEntry(GET_ONLY, "/auth/actuator/health"),
			new WhitelistEntry(GET_ONLY, "/auth/actuator/prometheus"),
			new WhitelistEntry(GET_ONLY, "/queue/actuator/health"),
			new WhitelistEntry(GET_ONLY, "/queue/actuator/prometheus"),
			new WhitelistEntry(GET_ONLY, "/seat/actuator/health"),
			new WhitelistEntry(GET_ONLY, "/seat/actuator/prometheus"),
			new WhitelistEntry(GET_ONLY, "/order/actuator/health"),
			new WhitelistEntry(GET_ONLY, "/order/actuator/prometheus"),

			// 공개 조회 API — GET 전용
			new WhitelistEntry(GET_ONLY, "/seat/blocks"),
			new WhitelistEntry(GET_ONLY, "/order/clubs"),
			new WhitelistEntry(GET_ONLY, "/order/matches")
	);

	private record WhitelistEntry(Set<HttpMethod> methods, String pathPrefix) {
	}

	private final JwtTokenProvider jwtTokenProvider;
	private final AccessTokenBlacklistRepository blacklistRepository;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		HttpMethod method = exchange.getRequest().getMethod();

		if (isWhitelisted(method, path)) {
			return chain.filter(exchange);
		}

		String token = resolveToken(exchange.getRequest());
		if (token == null) {
			log.debug("No JWT token found for path: {}", path);
			return unauthorizedResponse(exchange);
		}

		try {
			Claims claims = jwtTokenProvider.parseClaims(token);

			if (jwtTokenProvider.getTokenType(claims) != TokenType.ACCESS) {
				log.debug("Token type is not ACCESS for path: {}", path);
				return unauthorizedResponse(exchange);
			}

			String jti = jwtTokenProvider.getJti(claims);
			Long userId = jwtTokenProvider.getUserId(claims);
			String authority = jwtTokenProvider.getAuthority(claims);
			String sid = jwtTokenProvider.getSid(claims);

			return blacklistRepository.isBlacklisted(jti)
					.flatMap(isBlacklisted -> {
						if (isBlacklisted) {
							log.debug("Blacklisted token - jti: {}", jti);
							return unauthorizedResponse(exchange);
						}

						ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
								.header(HEADER_USER_ID, String.valueOf(userId))
								.header(HEADER_USER_ROLE, authority)
								.header(HEADER_TOKEN_JTI, jti);

						if (sid != null) {
							requestBuilder.header(HEADER_SESSION_ID, sid);
						}

						ServerHttpRequest mutatedRequest = requestBuilder.build();

						log.debug("JWT authenticated - userId: {}, role: {}, sid: {}", userId, authority, sid);
						return chain.filter(exchange.mutate().request(mutatedRequest).build());
					});

		} catch (Exception e) {
			log.warn("JWT validation failed for path: {} - {}", path, e.getMessage());
			return unauthorizedResponse(exchange);
		}
	}

	@Override
	public int getOrder() {
		return -1;
	}

	private boolean isWhitelisted(HttpMethod method, String path) {
		if (method == null) {
			return false;
		}
		for (WhitelistEntry entry : WHITELIST) {
			if (!entry.methods().contains(method)) {
				continue;
			}
			String prefix = entry.pathPrefix();
			if (path.equals(prefix) || path.startsWith(prefix + "/")) {
				return true;
			}
		}
		return false;
	}

	private String resolveToken(ServerHttpRequest request) {
		String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			return bearerToken.substring(BEARER_PREFIX.length());
		}
		return null;
	}

	private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		return exchange.getResponse().setComplete();
	}
}
