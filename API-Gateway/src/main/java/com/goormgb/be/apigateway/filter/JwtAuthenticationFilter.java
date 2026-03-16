package com.goormgb.be.apigateway.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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

	// 인증 없이 통과시킬 경로 prefix 목록
	private static final List<String> WHITELIST = List.of(
			"/auth/kakao",
			"/auth/token/refresh",
			"/auth/dev/auth",
			"/swagger-ui",
			"/v3/api-docs",
			"/auth/v3/api-docs",
			"/queue/v3/api-docs",
			"/seat/v3/api-docs",
			"/order/v3/api-docs",
			"/recommendation/v3/api-docs",
			"/actuator",
			"/seat/blocks",
			"/order/clubs",
			"/order/matches"
	);

	private final JwtTokenProvider jwtTokenProvider;
	private final AccessTokenBlacklistRepository blacklistRepository;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getPath().value();

		if (isWhitelisted(path)) {
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

			return blacklistRepository.isBlacklisted(jti)
					.flatMap(isBlacklisted -> {
						if (isBlacklisted) {
							log.debug("Blacklisted token - jti: {}", jti);
							return unauthorizedResponse(exchange);
						}

						ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
								.header(HEADER_USER_ID, String.valueOf(userId))
								.header(HEADER_USER_ROLE, authority)
								.build();

						log.debug("JWT authenticated - userId: {}, role: {}", userId, authority);
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

	private boolean isWhitelisted(String path) {
		return WHITELIST.stream().anyMatch(path::startsWith);
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
