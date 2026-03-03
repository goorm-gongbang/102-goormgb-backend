package com.goormgb.be.queue.metrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Configuration
public class QueueMetricsConfig {

	@Bean
	public Counter queueEntriesCounter(MeterRegistry registry) {
		return Counter.builder("ticketing_queue_entries_total")
				.description("Users entered queue")
				.register(registry);
	}

	@Bean
	public Counter queueAbandonedCounter(MeterRegistry registry) {
		return Counter.builder("ticketing_queue_abandoned_total")
				.description("Users left queue")
				.register(registry);
	}

	@Bean
	public Timer queueWaitTimer(MeterRegistry registry) {
		return Timer.builder("ticketing_queue_wait_seconds")
				.publishPercentiles(0.5, 0.95, 0.99)
				.register(registry);
	}
}
