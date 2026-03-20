package com.goormgb.be.ordercore.payment.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.metrics.OrderMetricsService;
import com.goormgb.be.ordercore.metrics.enums.PaymentMethodType;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.payment.dto.request.CashReceiptCreateRequest;
import com.goormgb.be.ordercore.payment.dto.request.PaymentProcessRequest;
import com.goormgb.be.ordercore.payment.dto.response.CashReceiptCreateResponse;
import com.goormgb.be.ordercore.payment.dto.response.PaymentProcessResponse;
import com.goormgb.be.ordercore.payment.entity.CashReceipt;
import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;
import com.goormgb.be.ordercore.payment.repository.CashReceiptRepository;
import com.goormgb.be.ordercore.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

	// 무통장 입금 목업 계좌 정보
	private static final String ACCOUNT_BANK = "신한은행";
	private static final String ACCOUNT_NUMBER = "110-123-456789";
	private static final String ACCOUNT_HOLDER = "주식회사 구름공방";
	private static final int DEPOSIT_DEADLINE_DAYS = 3;

	private final OrderMetricsService orderMetricsService;
	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;
	private final CashReceiptRepository cashReceiptRepository;

	/**
	 * 결제 처리.
	 * - BANK_TRANSFER: 무통장 입금 계좌 안내(목업), 입금 대기 상태 유지
	 * - TOSS_PAY / KAKAO_PAY: 외부 PG 연동 없이 즉시 결제 완료 처리(목업)
	 */
	public PaymentProcessResponse processPayment(Long userId, Long orderId, PaymentProcessRequest request) {
		long start = System.nanoTime();

		// 결제 요청 유입 횟수 증가 (결제 프로세스 시작 시점)
		orderMetricsService.increasePaymentAttempts();

		Order order = findOrderAndValidateOwnership(userId, orderId);

		try {
			Preconditions.validate(
				order.getStatus() == OrderStatus.PAYMENT_PENDING,
				ErrorCode.PAYMENT_ALREADY_COMPLETED
			);

			Preconditions.validate(
				!paymentRepository.findByOrderId(orderId).isPresent(),
				ErrorCode.PAYMENT_ALREADY_COMPLETED
			);

			Payment payment = buildPayment(order, request.paymentMethod());
			paymentRepository.save(payment);

			if (request.paymentMethod() != PaymentMethod.BANK_TRANSFER) {
				// 간편결제(토스페이, 카카오페이) 목업 즉시 완료
				payment.complete();
				order.updateStatus(OrderStatus.PAID);
				log.info("[PaymentService] 간편결제 완료(목업) - orderId={}, method={}", orderId, request.paymentMethod());

				// 즉시 결제 완료 건수 증가 (간편결제 목업 성공)
				if (request.paymentMethod() == PaymentMethod.KAKAO_PAY) {
					orderMetricsService.increasePaymentSuccess(PaymentMethodType.KAKAOPAY);
				} else if (request.paymentMethod() == PaymentMethod.TOSS_PAY) {
					orderMetricsService.increasePaymentSuccess(PaymentMethodType.TOSSPAY);
				}
			} else {
				log.info("[PaymentService] 무통장 입금 계좌 안내 - orderId={}", orderId);
			}

			return PaymentProcessResponse.of(payment);
		} catch (CustomException e) {
			// 결제 처리 실패 건수 증가 (검증 실패, 중복 결제, 기타 예외)
			orderMetricsService.increasePaymentFail();
			throw e;
		} finally {
			// 주문 생성부터 결제 처리 완료까지 전체 실행 시간 기록 (엔드투엔드 처리 성능 및 병목 분석용)
			orderMetricsService.recordOrderProcessTime(Duration.ofNanos(System.nanoTime() - start));
		}
	}

	/**
	 * 현금영수증 신청.
	 * 결제가 완료된 주문에 한해 현금영수증을 신청한다.
	 */
	public CashReceiptCreateResponse createCashReceipt(Long userId, Long orderId, CashReceiptCreateRequest request) {
		Order order = findOrderAndValidateOwnership(userId, orderId);

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

		Preconditions.validate(
			!cashReceiptRepository.findByPaymentId(payment.getId()).isPresent(),
			ErrorCode.CASH_RECEIPT_ALREADY_EXISTS
		);

		CashReceipt cashReceipt = CashReceipt.builder()
			.payment(payment)
			.purpose(request.purpose())
			.number(request.number())
			.build();

		cashReceiptRepository.save(cashReceipt);

		log.info("[PaymentService] 현금영수증 신청 완료 - orderId={}, purpose={}", orderId, request.purpose());

		return CashReceiptCreateResponse.of(orderId, cashReceipt);
	}

	private Order findOrderAndValidateOwnership(Long userId, Long orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

		Preconditions.validate(
			order.getUser().getId().equals(userId),
			ErrorCode.ORDER_ACCESS_DENIED
		);

		return order;
	}

	private Payment buildPayment(Order order, PaymentMethod method) {
		if (method == PaymentMethod.BANK_TRANSFER) {
			Instant depositDeadline = Instant.now().plus(DEPOSIT_DEADLINE_DAYS, ChronoUnit.DAYS);

			return Payment.builder()
				.order(order)
				.paymentMethod(method)
				.accountBank(ACCOUNT_BANK)
				.accountNumber(ACCOUNT_NUMBER)
				.accountHolder(ACCOUNT_HOLDER)
				.depositDeadline(depositDeadline)
				.build();
		}

		return Payment.builder()
			.order(order)
			.paymentMethod(method)
			.build();
	}
}
