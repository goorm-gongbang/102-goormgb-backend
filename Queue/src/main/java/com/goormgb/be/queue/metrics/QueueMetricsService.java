package com.goormgb.be.queue.metrics;

import java.time.Duration;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueMetricsService {

	private final Counter queueEntriesCounter;
	private final Counter queueAbandonedCounter;
	private final Timer queueWaitTimer;

	/**
	 * 큐 진입 시 호출
	 */
	public void recordEntry() {
		queueEntriesCounter.increment();
	}

	/**
	 * 큐 이탈 (중도 포기) 시 호출
	 */
	public void recordAbandoned() {
		queueAbandonedCounter.increment();
	}

	/**
	 * 큐 대기 시간 기록
	 */
	public void recordWaitTime(Duration waitTime) {
		queueWaitTimer.record(waitTime);
	}
}
