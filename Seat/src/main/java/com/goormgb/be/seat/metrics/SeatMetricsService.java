package com.goormgb.be.seat.metrics;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.goormgb.be.seat.metrics.enums.FallbackType;
import com.goormgb.be.seat.metrics.enums.RecommendDegradeType;
import com.goormgb.be.seat.metrics.enums.SeatHoldFailReason;
import com.goormgb.be.seat.metrics.enums.SeatHoldMode;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatMetricsService {

	private static final String METRIC_RECOMMEND_TOTAL = "ticketing_seat_recommend_total";
	private static final String METRIC_RECOMMEND_SUCCESS_TOTAL = "ticketing_seat_recommend_success_total";
	private static final String METRIC_RECOMMEND_FAIL_TOTAL = "ticketing_seat_recommend_fail_total";
	private static final String METRIC_PROCESS_SECONDS = "ticketing_seat_process_seconds";
	private static final String METRIC_LOCK_WAIT_SECONDS = "ticketing_seat_lock_wait_seconds";

	private static final String METRIC_HOLD_ATTEMPT_TOTAL = "ticketing_seat_hold_attempt_total";
	private static final String METRIC_HOLD_SUCCESS_TOTAL = "ticketing_seat_hold_success_total";
	private static final String METRIC_HOLD_FAIL_TOTAL = "ticketing_seat_hold_fail_total";
	private static final String METRIC_HOLD_EXPIRED_TOTAL = "ticketing_seat_hold_expired_total";
	private static final String METRIC_RECOMMEND_DEGRADE_TOTAL = "ticketing_seat_recommend_degrade_total";
	private static final String METRIC_RECOMMEND_FALLBACK_TOTAL = "ticketing_seat_recommend_fallback_total";

	private final MeterRegistry meterRegistry;

	/**
	 * 추천 요청이 발생했을 때 전체 요청 횟수를 증가시킨다.
	 */
	public void increaseRecommendTotal() {
		meterRegistry.counter(METRIC_RECOMMEND_TOTAL).increment();
	}

	/**
	 * 추천이 성공했을 때 성공 횟수를 증가시킨다.
	 */
	public void increaseRecommendSuccess() {
		meterRegistry.counter(METRIC_RECOMMEND_SUCCESS_TOTAL).increment();
	}

	/**
	 * 추천이 실패했을 때 실패 횟수를 증가시킨다.
	 */
	public void increaseRecommendFail() {
		meterRegistry.counter(METRIC_RECOMMEND_FAIL_TOTAL).increment();
	}

	/**
	 * 추천 알고리즘의 실행 시간을 기록한다.
	 */
	public void recordProcessTime(Duration duration) {
		Timer.builder(METRIC_PROCESS_SECONDS)
			.description("Seat recommendation algorithm execution time")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(meterRegistry)
			.record(duration);
	}

	/**
	 * 분산락 획득까지의 대기 시간을 기록한다.
	 */
	public void recordLockWaitTime(Duration duration) {
		Timer.builder(METRIC_LOCK_WAIT_SECONDS)
			.description("Time waiting for Redisson distributed lock")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(meterRegistry)
			.record(duration);
	}

	/**
	 * 좌석 hold 시도 횟수를 mode 라벨과 함께 증가시킨다.
	 */
	public void increaseHoldAttempt(SeatHoldMode mode) {
		meterRegistry.counter(
			METRIC_HOLD_ATTEMPT_TOTAL,
			"mode", mode.getValue()
		).increment();
	}

	/**
	 * 좌석 hold 성공 횟수를 mode 라벨과 함께 증가시킨다.
	 */
	public void increaseHoldSuccess(SeatHoldMode mode) {
		meterRegistry.counter(
			METRIC_HOLD_SUCCESS_TOTAL,
			"mode", mode.getValue()
		).increment();
	}

	/**
	 * 좌석 hold 실패 횟수를 mode, reason 라벨과 함께 증가시킨다.
	 */
	public void increaseHoldFail(SeatHoldMode mode, SeatHoldFailReason reason) {
		meterRegistry.counter(
			METRIC_HOLD_FAIL_TOTAL,
			"mode", mode.getValue(),
			"reason", reason.getValue()
		).increment();
	}

	/**
	 * 좌석 hold TTL 만료 횟수를 mode 라벨과 함께 증가시킨다.
	 */
	public void increaseHoldExpired() {
		meterRegistry.counter(
			METRIC_HOLD_EXPIRED_TOTAL,
			// "mode", mode.getValue()
			"mode", "common"
		).increment();
	}

	/**
	 * 추천 degrade 분기 진입 횟수를 degrade_type 라벨과 함께 증가시킨다.
	 */
	public void increaseRecommendDegrade(RecommendDegradeType degradeType) {
		meterRegistry.counter(
			METRIC_RECOMMEND_DEGRADE_TOTAL,
			"degrade_type", degradeType.getValue()
		).increment();
	}

	/**
	 * 추천 fallback 분기 진입 횟수를 fallback_type 라벨과 함께 증가시킨다.
	 */
	public void increaseRecommendFallback(FallbackType fallbackType) {
		meterRegistry.counter(
			METRIC_RECOMMEND_FALLBACK_TOTAL,
			"fallback_type", fallbackType.getValue()
		).increment();
	}
}