package com.goormgb.be.recommendation.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CircuitBreakerMetricsRecorder {

	private final MeterRegistry registry;

	public void recordState(String circuitBreakerName, String state) {
		Counter.builder("resilience4j_circuitbreaker_state_total")
				.description("Circuit breaker state transitions")
				.tag("name", circuitBreakerName)
				.tag("state", state) // OPEN / CLOSED / HALF_OPEN
				.register(registry)
				.increment();
	}
}
