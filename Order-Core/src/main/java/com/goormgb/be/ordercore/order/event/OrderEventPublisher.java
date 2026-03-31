package com.goormgb.be.ordercore.order.event;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.OrderCancelledEvent;
import com.goormgb.be.ordercore.order.entity.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

	private final ApplicationEventPublisher applicationEventPublisher;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public void publishOrderCancelled(Order order, List<Long> matchSeatIds) {
		applicationEventPublisher.publishEvent(
			new OrderCancelledInternalEvent(order, matchSeatIds)
		);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderCancelled(OrderCancelledInternalEvent internalEvent) {
		Order order = internalEvent.order();

		OrderCancelledEvent event = OrderCancelledEvent.builder()
			.orderId(order.getId())
			.userId(order.getUser().getId())
			.matchId(order.getMatch().getId())
			.matchSeatIds(internalEvent.matchSeatIds())
			.cancellationFee(order.getCancellationFee())
			.refundedAmount(order.getRefundedAmount())
			.occurredAt(Instant.now())
			.build();

		kafkaTemplate.send(
			EventTopic.ORDER_CANCELLED,
			String.valueOf(order.getId()),
			event
		).whenComplete((result, ex) -> {
			if (ex != null) {
				log.error("[Kafka] 주문 취소 이벤트 발행 실패: orderId={}, error={}",
					order.getId(), ex.getMessage(), ex);
			} else {
				log.info("[Kafka] 주문 취소 이벤트 발행 성공: orderId={}, seatCount={}, offset={}",
					order.getId(),
					internalEvent.matchSeatIds().size(),
					result.getRecordMetadata().offset());
			}
		});
	}
}
