package com.goormgb.be.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

	@Mock
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
	@Mock
	private ReactiveValueOperations<String, String> valueOperations;
	@Mock
	private GatewayFilterChain chain;

	@Test
	@DisplayName("로그인 요청이 제한 이하면 통과한다")
	void allowsLoginRequestUnderLimit() {
		RateLimitingFilter filter = new RateLimitingFilter(reactiveRedisTemplate);
		MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.post("/auth/kakao/login")
				.header("X-Forwarded-For", "203.0.113.10")
				.build()
		);

		when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment("rate_limit:login:203.0.113.10")).thenReturn(Mono.just(1L));
		when(reactiveRedisTemplate.expire("rate_limit:login:203.0.113.10", Duration.ofSeconds(60)))
			.thenReturn(Mono.just(true));
		when(chain.filter(any())).thenReturn(Mono.empty());

		StepVerifier.create(filter.filter(exchange, chain))
			.verifyComplete();

		assertThat(exchange.getResponse().getStatusCode()).isNull();
		verify(chain).filter(any());
	}

	@Test
	@DisplayName("로그인 요청이 제한을 초과하면 429를 반환한다")
	void blocksLoginRequestOverLimit() {
		RateLimitingFilter filter = new RateLimitingFilter(reactiveRedisTemplate);
		MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.post("/auth/kakao/login")
				.header("X-Forwarded-For", "203.0.113.10")
				.build()
		);

		when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment("rate_limit:login:203.0.113.10")).thenReturn(Mono.just(11L));

		StepVerifier.create(filter.filter(exchange, chain))
			.verifyComplete();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
		verify(chain, never()).filter(any());
	}

	@Test
	@DisplayName("일반 API 요청이 제한을 초과하면 429를 반환한다")
	void blocksGeneralApiRequestOverLimit() {
		RateLimitingFilter filter = new RateLimitingFilter(reactiveRedisTemplate);
		MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/order/matches/1")
				.header("X-Forwarded-For", "198.51.100.20")
				.build()
		);

		when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment("rate_limit:api:198.51.100.20")).thenReturn(Mono.just(101L));

		StepVerifier.create(filter.filter(exchange, chain))
			.verifyComplete();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		verify(chain, never()).filter(any());
	}

	@Test
	@DisplayName("Redis 오류가 발생하면 요청을 통과시킨다(fail-open)")
	void allowsRequestWhenRedisFails() {
		RateLimitingFilter filter = new RateLimitingFilter(reactiveRedisTemplate);
		MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.post("/auth/kakao/login")
				.header("X-Forwarded-For", "203.0.113.10")
				.build()
		);

		when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment(anyString())).thenReturn(Mono.error(new RuntimeException("redis down")));
		when(chain.filter(any())).thenReturn(Mono.empty());

		StepVerifier.create(filter.filter(exchange, chain))
			.verifyComplete();

		assertThat(exchange.getResponse().getStatusCode()).isNull();
		verify(chain).filter(any());
	}

	@Test
	@DisplayName("필터 순서는 JwtAuthenticationFilter보다 빠르다")
	void filterOrderIsBeforeJwtFilter() {
		RateLimitingFilter filter = new RateLimitingFilter(reactiveRedisTemplate);
		assertThat(filter.getOrder()).isEqualTo(-3);
	}
}
