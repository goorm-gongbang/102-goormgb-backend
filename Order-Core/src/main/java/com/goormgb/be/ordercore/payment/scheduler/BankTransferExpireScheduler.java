package com.goormgb.be.ordercore.payment.scheduler;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.query.SeatInfoQueryService;
import com.goormgb.be.ordercore.order.repository.OrderSeatRepository;
import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.enums.PaymentStatus;
import com.goormgb.be.ordercore.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 무통장 입금 기한 만료 시 자동 취소 스케줄러.
 *
 * <p>입금 기한(depositDeadline)이 지났는데 여전히 PENDING 상태인 무통장 입금 건을 찾아
 * 주문을 자동 취소하고 좌석을 AVAILABLE로 복원한다.</p>
 *
 * <h3>입금 기한 규칙</h3>
 * <ul>
 *   <li>일반: 다음날 23:59 KST</li>
 *   <li>경기 당일 예매: 경기 시작 3시간 전</li>
 *   <li>둘 중 빠른 시각 적용</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankTransferExpireScheduler {

	private final PaymentRepository paymentRepository;
	private final OrderSeatRepository orderSeatRepository;
	private final SeatInfoQueryService seatInfoQueryService;

	/**
	 * 5분마다 만료된 무통장 입금 건을 조회하여 자동 취소한다.
	 */
	@Scheduled(fixedDelay = 300_000)
	@Transactional
	public void cancelExpiredBankTransfers() {
		Instant now = Instant.now();
		List<Payment> expiredPayments = paymentRepository.findExpiredBankTransfers(PaymentStatus.PENDING, now);

		if (expiredPayments.isEmpty()) {
			return;
		}

		int cancelledCount = 0;
		for (Payment payment : expiredPayments) {
			Order order = payment.getOrder();

			if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
				continue;
			}

			// 주문 취소 + 결제 취소
			order.updateStatus(OrderStatus.CANCELLED);
			payment.cancel();

			// 좌석 SOLD → AVAILABLE 복원
			List<Long> matchSeatIds = orderSeatRepository.findMatchSeatIdsByOrderId(order.getId());
			seatInfoQueryService.markAvailableIfSold(matchSeatIds);

			cancelledCount++;
		}

		if (cancelledCount > 0) {
			log.info("[BankTransferExpireScheduler] 무통장 입금 기한 만료 자동 취소: {}건", cancelledCount);
		}
	}
}
