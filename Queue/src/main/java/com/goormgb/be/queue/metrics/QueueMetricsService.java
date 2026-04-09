package com.goormgb.be.queue.metrics;

import java.time.Duration;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueMetricsService {

	private static final String METRIC_QUEUE_ENTRIES_TOTAL = "ticketing_queue_entries_total";
	private static final String METRIC_QUEUE_ABANDONED_TOTAL = "ticketing_queue_abandoned_total";
	private static final String METRIC_QUEUE_WAIT_SECONDS = "ticketing_queue_wait_seconds";

	private final MeterRegistry meterRegistry;

	/**
	 * 큐 진입 시 호출
	 */
	public void recordEntry() {
		meterRegistry.counter(METRIC_QUEUE_ENTRIES_TOTAL).increment();
	}

	/**
	 * 큐 이탈 (중도 포기) 시 호출
	 */
	public void recordAbandoned() {
		meterRegistry.counter(METRIC_QUEUE_ABANDONED_TOTAL).increment();
	}

	/**
	 * 큐 대기 시간 기록
	 */
	public void recordWaitTime(Duration waitTime) {
		Timer.builder(METRIC_QUEUE_WAIT_SECONDS)
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(meterRegistry)
			.record(waitTime);
	}
}
