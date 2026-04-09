package com.goormgb.be.ordercore.payment.event;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.BankTransferExpiredEvent;
import com.goormgb.be.kafka.event.PaymentCompletedEvent;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.payment.entity.Payment;

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
			new PaymentCompletedInternalEvent(
				order.getId(),
				order.getUser().getId(),
				order.getMatch().getId(),
				matchSeatIds,
				order.getTotalAmount(),
				paymentMethod,
				Instant.now()
			)
		);
	}

	/**
	 * 무통장 입금 만료 이벤트를 발행한다.
	 * 트랜잭션 커밋 후 Kafka로 전송된다.
	 */
	public void publishBankTransferExpired(Order order, Payment payment, List<Long> matchSeatIds) {
		applicationEventPublisher.publishEvent(
			new BankTransferExpiredInternalEvent(
				order.getId(),
				order.getUser().getId(),
				order.getMatch().getId(),
				payment.getId(),
				matchSeatIds,
				Instant.now()
			)
		);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePaymentCompleted(PaymentCompletedInternalEvent internalEvent) {
		PaymentCompletedEvent event = PaymentCompletedEvent.builder()
			.orderId(internalEvent.orderId())
			.userId(internalEvent.userId())
			.matchId(internalEvent.matchId())
			.matchSeatIds(internalEvent.matchSeatIds())
			.paymentMethod(internalEvent.paymentMethod())
			.totalAmount(internalEvent.totalAmount())
			.occurredAt(internalEvent.occurredAt())
			.build();

		sendEvent(EventTopic.PAYMENT_COMPLETED, internalEvent.orderId(), event,
			"결제 완료", internalEvent.matchSeatIds().size());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleBankTransferExpired(BankTransferExpiredInternalEvent internalEvent) {
		BankTransferExpiredEvent event = BankTransferExpiredEvent.builder()
			.orderId(internalEvent.orderId())
			.userId(internalEvent.userId())
			.matchId(internalEvent.matchId())
			.paymentId(internalEvent.paymentId())
			.matchSeatIds(internalEvent.matchSeatIds())
			.occurredAt(internalEvent.occurredAt())
			.build();

		sendEvent(EventTopic.BANK_TRANSFER_EXPIRED, internalEvent.orderId(), event,
			"무통장 입금 만료", internalEvent.matchSeatIds().size());
	}

	private void sendEvent(String topic, Long orderId, Object event, String eventName, int seatCount) {
		kafkaTemplate.send(topic, String.valueOf(orderId), event)
			.whenComplete((SendResult<String, Object> result, Throwable ex) -> {
				if (ex != null) {
					log.error("[Kafka] {} 이벤트 발행 실패: orderId={}, error={}",
						eventName, orderId, ex.getMessage(), ex);
				} else {
					log.info("[Kafka] {} 이벤트 발행 성공: orderId={}, seatCount={}, offset={}",
						eventName, orderId, seatCount, result.getRecordMetadata().offset());
				}
			});
	}
}
