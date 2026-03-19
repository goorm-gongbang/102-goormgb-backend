package com.goormgb.be.ordercore.metrics;

import java.time.Duration;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderMetricsService {

	private final Counter paymentAttemptsCounter;
	private final Counter paymentSuccessCounter;
	private final Counter paymentFailCounter;
	private final Timer paymentLatencyTimer;
	private final Timer orderProcessTimer;

	/**
	 * 결제 시도 횟수 증가
	 */
	public void increasePaymentAttempts() {
		paymentAttemptsCounter.increment();
	}

	/**
	 * 결제 성공 횟수 증가
	 */
	public void increasePaymentSuccess() {
		paymentSuccessCounter.increment();
	}

	/**
	 * 결제 실패 횟수 증가
	 */
	public void increasePaymentFail() {
		paymentFailCounter.increment();
	}

	/**
	 * PG(결제 게이트웨이) 응답 시간 기록
	 */
	public void recordPaymentLatency(Duration duration) {
		paymentLatencyTimer.record(duration);
	}

	/**
	 * 주문 생성 ~ 결제 완료까지 전체 처리 시간 기록
	 */
	public void recordOrderProcessTime(Duration duration) {
		orderProcessTimer.record(duration);
	}
}