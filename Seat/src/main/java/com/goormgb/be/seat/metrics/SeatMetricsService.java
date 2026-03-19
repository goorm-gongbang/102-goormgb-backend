package com.goormgb.be.seat.metrics;

import java.time.Duration;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatMetricsService {

	private final Counter seatRecommendTotalCounter;
	private final Counter seatRecommendSuccessCounter;
	private final Counter seatRecommendFailCounter;
	private final Timer seatProcessTimer;
	private final Timer seatLockWaitTimer;

	/**
	 * 추천 요청 발생
	 */
	public void increaseRecommendTotal() {
		seatRecommendTotalCounter.increment();
	}

	/**
	 * 추천 성공
	 */
	public void increaseRecommendSuccess() {
		seatRecommendSuccessCounter.increment();
	}

	/**
	 * 추천 실패
	 */
	public void increaseRecommendFail() {
		seatRecommendFailCounter.increment();
	}

	/**
	 * 추천 알고리즘 실행 시간 기록
	 */
	public void recordProcessTime(Duration duration) {
		seatProcessTimer.record(duration);
	}

	/**
	 * 분산락 대기 시간 기록 (Redisson)
	 */
	public void recordLockWaitTime(Duration duration) {
		seatLockWaitTimer.record(duration);
	}
}