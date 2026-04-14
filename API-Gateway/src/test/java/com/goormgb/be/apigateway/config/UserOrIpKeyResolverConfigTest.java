package com.goormgb.be.apigateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class UserOrIpKeyResolverConfigTest {

	private final UserOrIpKeyResolverConfig config = new UserOrIpKeyResolverConfig();
	private final KeyResolver keyResolver = config.userOrIpKeyResolver();

	@Test
	@DisplayName("X-User-Id가 있으면 uid 키를 사용한다")
	void resolvesByUserId() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.post("/queue/matches/1/enter")
				.header("X-User-Id", "42")
				.header("X-Forwarded-For", "203.0.113.10")
				.build()
		);

		String key = keyResolver.resolve(exchange).block();

		assertThat(key).isEqualTo("uid:42");
	}

	@Test
	@DisplayName("X-User-Id가 없으면 X-Forwarded-For의 첫 번째 IP를 사용한다")
	void resolvesByXForwardedFor() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.post("/queue/matches/1/enter")
				.header("X-Forwarded-For", "203.0.113.10, 198.51.100.20")
				.build()
		);

		String key = keyResolver.resolve(exchange).block();

		assertThat(key).isEqualTo("ip:203.0.113.10");
	}

	@Test
	@DisplayName("헤더가 없으면 remoteAddress 기반 IP를 사용한다")
	void resolvesByRemoteAddress() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.post("/queue/matches/1/enter")
				.remoteAddress(new InetSocketAddress("127.0.0.1", 12345))
				.build()
		);

		String key = keyResolver.resolve(exchange).block();

		assertThat(key).isEqualTo("ip:127.0.0.1");
	}
}
