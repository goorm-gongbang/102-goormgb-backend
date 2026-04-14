package com.goormgb.be.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.goormgb.be.apigateway.config.UserOrIpKeyResolverConfig;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RateLimitMonitoringFilterTest {

	@Mock
	private GatewayFilterChain chain;

	private final RateLimitMonitoringFilter filter = new RateLimitMonitoringFilter();

	private final Logger logger = (Logger)LoggerFactory.getLogger(RateLimitMonitoringFilter.class);
	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

	@AfterEach
	void tearDown() {
		logger.detachAppender(appender);
		appender.stop();
	}

	@Test
	@DisplayName("429 응답이면 key, path, clientIp 로그를 남긴다")
	void logsOn429() {
		appender.start();
		logger.addAppender(appender);

		MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.post("/queue/matches/1/enter")
				.header("X-Forwarded-For", "203.0.113.10")
				.build()
		);
		exchange.getAttributes().put(UserOrIpKeyResolverConfig.ATTR_RATE_LIMIT_KEY, "uid:42");

		when(chain.filter(any())).thenReturn(Mono.fromRunnable(
			() -> exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS)));

		StepVerifier.create(filter.filter(exchange, chain))
			.verifyComplete();

		List<ILoggingEvent> logs = appender.list;
		assertThat(logs).hasSize(1);
		assertThat(logs.getFirst().getLevel()).isEqualTo(Level.WARN);
		assertThat(logs.getFirst().getFormattedMessage())
			.contains("Rate limit hit(429)")
			.contains("key=uid:42")
			.contains("path=/queue/matches/1/enter")
			.contains("clientIp=203.0.113.10");
	}

	@Test
	@DisplayName("429가 아니면 로그를 남기지 않는다")
	void doesNotLogWhenNot429() {
		appender.start();
		logger.addAppender(appender);

		MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.post("/queue/matches/1/enter").build()
		);

		when(chain.filter(any())).thenReturn(Mono.fromRunnable(
			() -> exchange.getResponse().setStatusCode(HttpStatus.OK)));

		StepVerifier.create(filter.filter(exchange, chain))
			.verifyComplete();

		assertThat(appender.list).isEmpty();
	}
}
