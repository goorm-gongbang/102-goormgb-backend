package com.goormgb.be.ordercore.payment.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.metrics.OrderMetricsService;
import com.goormgb.be.ordercore.metrics.enums.PaymentMethodType;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.query.SeatInfoQueryService;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.order.repository.OrderSeatRepository;
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

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	// 무통장 입금 목업 계좌 정보
	private static final String ACCOUNT_BANK = "신한은행";
	private static final String ACCOUNT_NUMBER = "110-123-456789";
	private static final String ACCOUNT_HOLDER = "주식회사 구름공방";

	// 무통장 입금 기한: 다음날 23:59 KST, 경기 당일이면 경기 3시간 전
	private static final Duration MATCH_DAY_DEADLINE_BEFORE = Duration.ofHours(3);

	private final Clock clock;
	private final OrderMetricsService orderMetricsService;
	private final OrderRepository orderRepository;
	private final OrderSeatRepository orderSeatRepository;
	private final PaymentRepository paymentRepository;
	private final CashReceiptRepository cashReceiptRepository;
	private final SeatInfoQueryService seatInfoQueryService;

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

			if (request.paymentMethod() == PaymentMethod.BANK_TRANSFER) {
				Instant matchDeadline = order.getMatch().getMatchAt().minus(MATCH_DAY_DEADLINE_BEFORE);
				Preconditions.validate(
					clock.instant().isBefore(matchDeadline),
					ErrorCode.BANK_TRANSFER_NOT_AVAILABLE
				);
			}

			Payment payment = buildPayment(order, request.paymentMethod());
			paymentRepository.save(payment);

			// 현금영수증 신청 정보가 있으면 함께 저장
			if (request.hasCashReceipt()) {
				CashReceipt cashReceipt = CashReceipt.builder()
					.payment(payment)
					.purpose(request.cashReceiptPurpose())
					.number(request.cashReceiptNumber())
					.build();
				cashReceiptRepository.save(cashReceipt);
				log.info("[PaymentService] 현금영수증 신청 완료 - orderId={}, purpose={}", orderId, request.cashReceiptPurpose());
			}

			// 결제 수단과 무관하게 좌석을 SOLD로 전환 (스케줄러가 풀지 못하도록)
			markSeatsAsSold(orderId);

			if (request.paymentMethod() != PaymentMethod.BANK_TRANSFER) {
				// 간편결제(토스페이, 카카오페이) 목업 즉시 완료
				payment.complete();
				order.updateStatus(OrderStatus.PAID);
				log.info("[PaymentService] 간편결제 완료(목업) - orderId={}, method={}", orderId, request.paymentMethod());

				// 즉시 결제 완료 건수 증가 (간편결제 목업 성공)
				switch (request.paymentMethod()) {
					case KAKAO_PAY -> orderMetricsService.increasePaymentSuccess(PaymentMethodType.KAKAOPAY);
					case TOSS_PAY -> orderMetricsService.increasePaymentSuccess(PaymentMethodType.TOSSPAY);
					default -> log.warn("[PaymentService] 알 수 없는 간편결제 타입에 대한 성공 메트릭이 누락되었습니다. - method={}", request.paymentMethod());
				}
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
	 * 현금영수증 신청 (목업).
	 * 결제 상태와 무관하게 현금영수증 정보를 저장한다.
	 */
	public CashReceiptCreateResponse createCashReceipt(Long userId, Long orderId, CashReceiptCreateRequest request) {
		Order order = findOrderAndValidateOwnership(userId, orderId);

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

		CashReceipt cashReceipt = cashReceiptRepository.findByPaymentId(payment.getId())
			.orElse(null);

		if (cashReceipt != null) {
			cashReceipt.update(request.purpose(), request.number());
			log.info("[PaymentService] 현금영수증 정보 수정 - orderId={}, purpose={}", orderId, request.purpose());
		} else {
			cashReceipt = CashReceipt.builder()
				.payment(payment)
				.purpose(request.purpose())
				.number(request.number())
				.build();
			cashReceiptRepository.save(cashReceipt);
			log.info("[PaymentService] 현금영수증 신청 완료 - orderId={}, purpose={}", orderId, request.purpose());
		}

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

	private void markSeatsAsSold(Long orderId) {
		List<Long> matchSeatIds = orderSeatRepository.findMatchSeatIdsByOrderId(orderId);
		int updated = seatInfoQueryService.markSoldIfBlocked(matchSeatIds);
		log.info("[PaymentService] 좌석 SOLD 전환 - orderId={}, count={}", orderId, updated);
		if (updated != matchSeatIds.size()) {
			log.warn("[PaymentService] 좌석 SOLD 전환 개수 불일치 - orderId={}, expected={}, updated={}",
				orderId, matchSeatIds.size(), updated);
		}
	}

	private Payment buildPayment(Order order, PaymentMethod method) {
		if (method == PaymentMethod.BANK_TRANSFER) {
			Instant depositDeadline = calculateDepositDeadline(order.getMatch().getMatchAt());

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

	/**
	 * 무통장 입금 기한을 계산한다.
	 * - 일반: 다음날 23:59 KST
	 * - 경기 당일 예매: 경기 시작 3시간 전
	 * - 둘 중 빠른 시각을 적용
	 */
	private Instant calculateDepositDeadline(Instant matchAt) {
		ZonedDateTime now = clock.instant().atZone(KST);

		// 일반 기한: 다음날 23:59 KST
		Instant tomorrowEnd = now.toLocalDate().plusDays(1)
			.atTime(LocalTime.of(23, 59))
			.atZone(KST)
			.toInstant();

		// 경기 기한: 경기 시작 3시간 전
		Instant matchDeadline = matchAt.minus(MATCH_DAY_DEADLINE_BEFORE);

		// 둘 중 빠른 시각 적용
		return tomorrowEnd.isBefore(matchDeadline) ? tomorrowEnd : matchDeadline;
	}
}
