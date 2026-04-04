package com.goormgb.be.ordercore.email.event;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.OrderCancelledEvent;
import com.goormgb.be.kafka.event.PaymentCompletedEvent;
import com.goormgb.be.ordercore.email.service.EmailDataQueryService;
import com.goormgb.be.ordercore.email.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventConsumer {

	private final EmailService emailService;
	private final EmailDataQueryService emailDataQueryService;

	@KafkaListener(
		topics = EventTopic.PAYMENT_COMPLETED,
		groupId = "${spring.kafka.consumer.group-id}"
	)
	public void handlePaymentCompleted(PaymentCompletedEvent event) {
		Map<String, Object> context = emailDataQueryService
			.buildPaymentEmailContext(event.getOrderId(), event)
			.orElse(null);

		if (context == null) {
			log.warn("[Kafka-Email] 주문 없음, 이메일 발송 스킵: orderId={}", event.getOrderId());
			return;
		}

		emailService.sendPaymentConfirmation(context);
	}

	@KafkaListener(
		topics = EventTopic.ORDER_CANCELLED,
		groupId = "${spring.kafka.consumer.group-id}"
	)
	public void handleOrderCancelled(OrderCancelledEvent event) {
		Map<String, Object> context = emailDataQueryService
			.buildCancellationEmailContext(event.getOrderId(), event)
			.orElse(null);

		if (context == null) {
			log.warn("[Kafka-Email] 주문 없음, 이메일 발송 스킵: orderId={}", event.getOrderId());
			return;
		}

		emailService.sendCancellationConfirmation(context);
	}
}
