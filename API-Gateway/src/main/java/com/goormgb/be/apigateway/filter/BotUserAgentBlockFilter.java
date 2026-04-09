package com.goormgb.be.apigateway.filter;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class BotUserAgentBlockFilter implements GlobalFilter, Ordered {

	private static final Pattern QUEUE_ENTER_PATH = Pattern.compile("^/queue/matches/\\d+/enter$");
	private static final List<String> BLOCKED_USER_AGENTS = List.of(
		"curl",
		"wget",
		"python",
		"selenium",
		"puppeteer",
		"playwright",
		"headless"
	);
	private static final int MIN_BROWSER_UA_LENGTH = 20;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();

		if (!isQueueEnterRequest(request)) {
			return chain.filter(exchange);
		}

		String userAgent = request.getHeaders().getFirst("User-Agent");
		if (!isBlockedUserAgent(userAgent)) {
			return chain.filter(exchange);
		}

		log.warn(
			"Blocked queue enter request due to suspicious user agent. path={}, userAgent={}, reasonCode=AUTOMATION_UA_BLOCKED",
			request.getPath().value(),
			userAgent
		);

		exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
		return exchange.getResponse().setComplete();
	}

	@Override
	public int getOrder() {
		return -2;
	}

	private boolean isQueueEnterRequest(ServerHttpRequest request) {
		return HttpMethod.POST.equals(request.getMethod())
			&& QUEUE_ENTER_PATH.matcher(request.getPath().value()).matches();
	}

	private boolean isBlockedUserAgent(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return true;
		}

		String normalized = userAgent.toLowerCase(Locale.ROOT);
		if (normalized.length() < MIN_BROWSER_UA_LENGTH) {
			return true;
		}

		return BLOCKED_USER_AGENTS.stream().anyMatch(normalized::contains);
	}
}
