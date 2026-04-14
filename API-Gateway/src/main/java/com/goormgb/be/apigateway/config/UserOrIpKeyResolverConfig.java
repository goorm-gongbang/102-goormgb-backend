package com.goormgb.be.apigateway.config;

import java.net.InetSocketAddress;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Configuration
public class UserOrIpKeyResolverConfig {

	private static final String HEADER_USER_ID = "X-User-Id";
	private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
	public static final String ATTR_RATE_LIMIT_KEY = "rateLimit.resolvedKey";

	@Bean("userOrIpKeyResolver")
	public KeyResolver userOrIpKeyResolver() {
		return exchange -> Mono.fromSupplier(() -> {
			String resolvedKey = resolveKey(exchange);
			exchange.getAttributes().put(ATTR_RATE_LIMIT_KEY, resolvedKey);
			return resolvedKey;
		});
	}

	private String resolveKey(ServerWebExchange exchange) {
		String userId = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ID);
		if (StringUtils.hasText(userId)) {
			return "uid:" + userId.trim();
		}

		String xForwardedFor = exchange.getRequest().getHeaders().getFirst(HEADER_X_FORWARDED_FOR);
		if (StringUtils.hasText(xForwardedFor)) {
			String firstIp = xForwardedFor.split(",")[0].trim();
			if (StringUtils.hasText(firstIp)) {
				return "ip:" + firstIp;
			}
		}

		InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
		if (remoteAddress != null) {
			if (remoteAddress.getAddress() != null && StringUtils.hasText(remoteAddress.getAddress().getHostAddress())) {
				return "ip:" + remoteAddress.getAddress().getHostAddress();
			}
			if (StringUtils.hasText(remoteAddress.getHostString())) {
				return "ip:" + remoteAddress.getHostString();
			}
		}

		return "ip:unknown";
	}
}
