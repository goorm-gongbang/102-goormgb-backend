package com.goormgb.be.seat.metrics;

import java.util.function.Supplier;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

// @Component // TODO: seat hold 기능 구현 후 활성화
@RequiredArgsConstructor
public class SeatHoldMetricsRecorder {

	private final MeterRegistry registry;
	private final Supplier<Number> holdCountSupplier;

	@PostConstruct
	public void registerActiveSeatHoldGauge() {
		Gauge.builder("ticketing_seat_hold_token_active", holdCountSupplier)
				.description("Current active temporary seat holds (TTL active)")
				.register(registry);
	}
}