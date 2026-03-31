package com.goormgb.be.ordercore.payment.event;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.PaymentCompletedEvent;
import com.goormgb.be.ordercore.order.entity.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

	private final ApplicationEventPublisher applicationEventPublisher;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	/**
	 * 결제 완료 이벤트를 발행한다.
	 * 트랜잭션 커밋 후 Kafka로 전송된다.
	 */
	public void publishPaymentCompleted(Order order, List<Long> matchSeatIds, String paymentMethod) {
		applicationEventPublisher.publishEvent(
			new PaymentCompletedInternalEvent(order, matchSeatIds, paymentMethod)
		);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePaymentCompleted(PaymentCompletedInternalEvent internalEvent) {
		Order order = internalEvent.order();

		PaymentCompletedEvent event = PaymentCompletedEvent.builder()
			.orderId(order.getId())
			.userId(order.getUser().getId())
			.matchId(order.getMatch().getId())
			.matchSeatIds(internalEvent.matchSeatIds())
			.paymentMethod(internalEvent.paymentMethod())
			.totalAmount(order.getTotalAmount())
			.occurredAt(Instant.now())
			.build();

		kafkaTemplate.send(
			EventTopic.PAYMENT_COMPLETED,
			String.valueOf(order.getId()),
			event
		).whenComplete((result, ex) -> {
			if (ex != null) {
				log.error("[Kafka] 결제 완료 이벤트 발행 실패: orderId={}, error={}",
					order.getId(), ex.getMessage(), ex);
			} else {
				log.info("[Kafka] 결제 완료 이벤트 발행 성공: orderId={}, seatCount={}, offset={}",
					order.getId(),
					internalEvent.matchSeatIds().size(),
					result.getRecordMetadata().offset());
			}
		});
	}
}
