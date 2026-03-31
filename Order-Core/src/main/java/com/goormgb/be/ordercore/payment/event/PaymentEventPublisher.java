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
		PaymentCompletedEvent event = PaymentCompletedEvent.builder()
			.orderId(internalEvent.getOrder().getId())
			.userId(internalEvent.getOrder().getUser().getId())
			.matchId(internalEvent.getOrder().getMatch().getId())
			.matchSeatIds(internalEvent.getMatchSeatIds())
			.paymentMethod(internalEvent.getPaymentMethod())
			.totalAmount(internalEvent.getOrder().getTotalAmount())
			.occurredAt(Instant.now())
			.build();

		kafkaTemplate.send(
			EventTopic.PAYMENT_COMPLETED,
			String.valueOf(internalEvent.getOrder().getId()),
			event
		).whenComplete((result, ex) -> {
			if (ex != null) {
				log.error("[Kafka] 결제 완료 이벤트 발행 실패: orderId={}, error={}",
					internalEvent.getOrder().getId(), ex.getMessage(), ex);
			} else {
				log.info("[Kafka] 결제 완료 이벤트 발행 성공: orderId={}, seatCount={}, offset={}",
					internalEvent.getOrder().getId(),
					internalEvent.getMatchSeatIds().size(),
					result.getRecordMetadata().offset());
			}
		});
	}
}
