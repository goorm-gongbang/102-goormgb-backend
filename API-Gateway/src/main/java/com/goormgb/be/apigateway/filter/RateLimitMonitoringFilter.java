package com.goormgb.be.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.goormgb.be.apigateway.config.UserOrIpKeyResolverConfig;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RateLimitMonitoringFilter implements GlobalFilter, Ordered {

	private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		return chain.filter(exchange)
			.then(Mono.fromRunnable(() -> logIfRateLimitHit(exchange)));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	private void logIfRateLimitHit(ServerWebExchange exchange) {
		HttpStatusCode status = exchange.getResponse().getStatusCode();
		if (status == null || status.value() != 429) {
			return;
		}

		String key = exchange.getAttribute(UserOrIpKeyResolverConfig.ATTR_RATE_LIMIT_KEY);
		String path = exchange.getRequest().getPath().value();
		String clientIp = resolveClientIp(exchange);

		log.warn("Rate limit hit(429) key={}, path={}, clientIp={}",
			StringUtils.hasText(key) ? key : "unknown",
			path,
			clientIp);
	}

	private String resolveClientIp(ServerWebExchange exchange) {
		String xForwardedFor = exchange.getRequest().getHeaders().getFirst(HEADER_X_FORWARDED_FOR);
		if (StringUtils.hasText(xForwardedFor)) {
			String firstIp = xForwardedFor.split(",")[0].trim();
			if (StringUtils.hasText(firstIp)) {
				return firstIp;
			}
		}

		if (exchange.getRequest().getRemoteAddress() != null) {
			if (exchange.getRequest().getRemoteAddress().getAddress() != null
				&& StringUtils.hasText(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress())) {
				return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
			}
			if (StringUtils.hasText(exchange.getRequest().getRemoteAddress().getHostString())) {
				return exchange.getRequest().getRemoteAddress().getHostString();
			}
		}

		return "unknown";
	}
}
