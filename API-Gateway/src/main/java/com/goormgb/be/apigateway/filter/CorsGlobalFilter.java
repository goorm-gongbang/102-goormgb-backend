package com.goormgb.be.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class CorsGlobalFilter implements WebFilter, Ordered {

	@Value("${ALLOWED_ORIGINS:*}")
	private String allowedOrigins;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String origin = exchange.getRequest().getHeaders().getOrigin();

		if (origin != null && isAllowed(origin)) {
			HttpHeaders headers = exchange.getResponse().getHeaders();
			headers.set("Access-Control-Allow-Origin", origin);
			headers.set("Access-Control-Allow-Credentials", "true");
			headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
			headers.set("Access-Control-Allow-Headers", "*");
			headers.set("Vary", "Origin");
		}

		if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
			exchange.getResponse().setStatusCode(HttpStatus.OK);
			return exchange.getResponse().setComplete();
		}

		return chain.filter(exchange);
	}

	private static final AntPathMatcher MATCHER = new AntPathMatcher();

	private boolean isAllowed(String origin) {
		if ("*".equals(allowedOrigins)) {
			return true;
		}
		for (String allowed : allowedOrigins.split(",")) {
			String pattern = allowed.trim();
			if (origin.equalsIgnoreCase(pattern) || MATCHER.match(pattern, origin)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
