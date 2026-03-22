package com.goormgb.be.ordercore.metrics;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.goormgb.be.ordercore.metrics.enums.OrderDraftEntryPoint;
import com.goormgb.be.ordercore.metrics.enums.PaymentMethodType;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderMetricsService {

	private static final String METRIC_PAYMENT_ATTEMPTS_TOTAL = "ticketing_payment_attempts_total";
	private static final String METRIC_PAYMENT_SUCCESS_TOTAL = "ticketing_payment_success_total";
	private static final String METRIC_PAYMENT_FAIL_TOTAL = "ticketing_payment_fail_total";
	private static final String METRIC_PAYMENT_LATENCY_SECONDS = "ticketing_payment_latency_seconds";
	private static final String METRIC_ORDER_PROCESS_SECONDS = "ticketing_order_process_seconds";

	private static final String METRIC_ORDER_DRAFT_ENTER_TOTAL = "ticketing_order_draft_enter_total";
	private static final String METRIC_PAYMENT_BANK_TRANSFER_EXPIRED_TOTAL =
		"ticketing_payment_bank_transfer_expired_total";

	private static final String TAG_ENTRY_POINT = "entry_point";
	private static final String TAG_METHOD = "method";

	private final MeterRegistry meterRegistry;

	/**
	 * 결제 시도 횟수를 증가시킨다.
	 */
	public void increasePaymentAttempts() {
		meterRegistry.counter(METRIC_PAYMENT_ATTEMPTS_TOTAL).increment();
	}

	/**
	 * 결제 성공 횟수를 결제 수단(method) 라벨과 함께 증가시킨다.
	 */
	public void increasePaymentSuccess(PaymentMethodType method) {
		meterRegistry.counter(
			METRIC_PAYMENT_SUCCESS_TOTAL,
			TAG_METHOD, method.getValue()
		).increment();
	}

	/**
	 * 결제 실패 횟수를 증가시킨다.
	 */
	public void increasePaymentFail() {
		meterRegistry.counter(METRIC_PAYMENT_FAIL_TOTAL).increment();
	}

	/**
	 * PG(결제 게이트웨이) 응답 시간을 기록한다.
	 */
	public void recordPaymentLatency(Duration duration) {
		Timer.builder(METRIC_PAYMENT_LATENCY_SECONDS)
			.description("Payment gateway response time")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(meterRegistry)
			.record(duration);
	}

	/**
	 * 주문 생성부터 결제 완료까지 전체 처리 시간을 기록한다.
	 */
	public void recordOrderProcessTime(Duration duration) {
		Timer.builder(METRIC_ORDER_PROCESS_SECONDS)
			.description("End-to-end order creation and payment latency")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(meterRegistry)
			.record(duration);
	}

	/**
	 * 주문 Draft 진입 횟수를 entry_point 라벨과 함께 증가시킨다.
	 */
	public void increaseOrderDraftEnter(OrderDraftEntryPoint entryPoint) {
		meterRegistry.counter(
			METRIC_ORDER_DRAFT_ENTER_TOTAL,
			TAG_ENTRY_POINT, entryPoint.getValue()
		).increment();
	}

	/**
	 * 무통장 입금 주문 만료 횟수를 증가시킨다.
	 */
	public void increaseBankTransferExpired() {
		meterRegistry.counter(METRIC_PAYMENT_BANK_TRANSFER_EXPIRED_TOTAL).increment();
	}
}