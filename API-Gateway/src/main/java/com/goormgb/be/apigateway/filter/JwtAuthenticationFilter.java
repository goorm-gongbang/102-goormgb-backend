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

	/**
	 * 화이트리스트: (허용 HTTP 메서드, 경로 prefix) 쌍으로 관리한다.
	 * methods가 비어 있으면 모든 메서드를 허용한다.
	 */
	private record WhitelistEntry(Set<String> methods, String pathPrefix) {
		boolean matches(String method, String path) {
			return path.startsWith(pathPrefix)
				&& (methods.isEmpty() || methods.contains(method));
		}
	}

	private static final Set<String> GET_ONLY = Set.of("GET");
	private static final Set<String> POST_ONLY = Set.of("POST");
	private static final Set<String> GET_POST = Set.of("GET", "POST");
	private static final Set<String> ANY_METHOD = Set.of();

	private static final List<WhitelistEntry> WHITELIST = List.of(
		// 인증 관련 — /auth/kakao는 GET(login-url) + POST(콜백) 모두 필요
		new WhitelistEntry(GET_POST, "/auth/kakao"),
		new WhitelistEntry(POST_ONLY, "/auth/token/refresh"),
		new WhitelistEntry(ANY_METHOD, "/auth/dev/auth"),
		new WhitelistEntry(ANY_METHOD, "/auth/loadtest"),

		// Swagger / API docs — GET만
		new WhitelistEntry(GET_ONLY, "/swagger-ui"),
		new WhitelistEntry(GET_ONLY, "/v3/api-docs"),
		new WhitelistEntry(GET_ONLY, "/auth/v3/api-docs"),
		new WhitelistEntry(GET_ONLY, "/queue/v3/api-docs"),
		new WhitelistEntry(GET_ONLY, "/seat/v3/api-docs"),
		new WhitelistEntry(GET_ONLY, "/order/v3/api-docs"),
		new WhitelistEntry(GET_ONLY, "/recommendation/v3/api-docs"),

		// Actuator — GET만
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

		// 공개 조회 API — GET만
		new WhitelistEntry(GET_ONLY, "/seat/blocks"),
		new WhitelistEntry(GET_ONLY, "/order/clubs"),
		new WhitelistEntry(GET_ONLY, "/order/matches")
	);

	private final JwtTokenProvider jwtTokenProvider;
	private final AccessTokenBlacklistRepository blacklistRepository;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		String method = exchange.getRequest().getMethod() != null
			? exchange.getRequest().getMethod().name()
			: "GET";

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

	private boolean isWhitelisted(String method, String path) {
		return WHITELIST.stream().anyMatch(entry -> entry.matches(method, path));
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
