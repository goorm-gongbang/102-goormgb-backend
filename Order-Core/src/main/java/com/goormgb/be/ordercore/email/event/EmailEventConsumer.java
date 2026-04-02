package com.goormgb.be.ordercore.email.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.OrderCancelledEvent;
import com.goormgb.be.kafka.event.PaymentCompletedEvent;
import com.goormgb.be.ordercore.email.service.EmailService;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventConsumer {

	private final EmailService emailService;
	private final OrderRepository orderRepository;

	@KafkaListener(
		topics = EventTopic.PAYMENT_COMPLETED,
		groupId = "order-core-notification"
	)
	public void handlePaymentCompleted(PaymentCompletedEvent event) {
		Order order = orderRepository.findById(event.getOrderId()).orElse(null);
		if (order == null) {
			log.warn("[Kafka-Email] 주문 없음, 이메일 발송 스킵: orderId={}", event.getOrderId());
			return;
		}

		emailService.sendPaymentConfirmation(order, event);
	}

	@KafkaListener(
		topics = EventTopic.ORDER_CANCELLED,
		groupId = "order-core-notification"
	)
	public void handleOrderCancelled(OrderCancelledEvent event) {
		Order order = orderRepository.findById(event.getOrderId()).orElse(null);
		if (order == null) {
			log.warn("[Kafka-Email] 주문 없음, 이메일 발송 스킵: orderId={}", event.getOrderId());
			return;
		}

		emailService.sendCancellationConfirmation(order, event);
	}
}
