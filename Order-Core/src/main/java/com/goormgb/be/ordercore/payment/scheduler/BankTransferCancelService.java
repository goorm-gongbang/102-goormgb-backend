package com.goormgb.be.ordercore.payment.scheduler;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.repository.OrderSeatRepository;
import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.event.PaymentEventPublisher;

import lombok.RequiredArgsConstructor;

/**
 * 무통장 입금 개별 취소 트랜잭션 서비스.
 *
 * <p>스케줄러에서 건별로 호출되며, 각 결제 취소가 독립 트랜잭션으로 처리된다.
 * 한 건의 실패가 다른 건에 영향을 주지 않도록 분리한다.</p>
 */
@Service
@RequiredArgsConstructor
public class BankTransferCancelService {

	private final OrderSeatRepository orderSeatRepository;
	private final PaymentEventPublisher paymentEventPublisher;

	/**
	 * 단일 무통장 입금 결제를 취소하고 좌석을 복원한다.
	 *
	 * @param payment 만료된 결제 엔티티 (Order가 FETCH JOIN된 상태)
	 * @return 취소 성공 여부 (이미 취소된 경우 false)
	 */
	@Transactional
	public boolean cancelSinglePayment(Payment payment) {
		Order order = payment.getOrder();

		if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
			return false;
		}

		order.updateStatus(OrderStatus.CANCELLED);
		payment.cancel();

		// 무통장 입금 만료 이벤트 발행 → Seat 서비스에서 좌석 SOLD → AVAILABLE 복원
		List<Long> matchSeatIds = orderSeatRepository.findMatchSeatIdsByOrderId(order.getId());
		paymentEventPublisher.publishBankTransferExpired(order, payment, matchSeatIds);
		orderSeatRepository.deleteByOrderId(order.getId());

		return true;
	}
}
