package com.goormgb.be.seat.metrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SeatMetricsConfig {

	@Bean
	public Counter seatRecommendTotalCounter(MeterRegistry registry) {
		return Counter.builder("ticketing_seat_recommend_total")
				.description("Total seat recommendation requests")
				.register(registry);
	}

	@Bean
	public Counter seatRecommendSuccessCounter(MeterRegistry registry) {
		return Counter.builder("ticketing_seat_recommend_success_total")
				.description("Successful seat recommendations")
				.register(registry);
	}

	@Bean
	public Counter seatRecommendFailCounter(MeterRegistry registry) {
		return Counter.builder("ticketing_seat_recommend_fail_total")
				.description("Failed seat recommendations")
				.register(registry);
	}

	@Bean
	public Timer seatProcessTimer(MeterRegistry registry) {
		return Timer.builder("ticketing_seat_process_seconds")
				.description("Seat recommendation algorithm execution time")
				.publishPercentiles(0.5, 0.95, 0.99)
				.register(registry);
	}

	@Bean
	public Timer seatLockWaitTimer(MeterRegistry registry) {
		return Timer.builder("ticketing_seat_lock_wait_seconds")
				.description("Time waiting for Redisson distributed lock")
				.publishPercentiles(0.5, 0.95, 0.99)
				.register(registry);
	}
}
