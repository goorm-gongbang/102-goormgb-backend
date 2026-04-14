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
		if (emailDataQueryService.isOrderPaid(event.getOrderId())) {
			Map<String, Object> bookingContext = emailDataQueryService
				.buildBookingEmailContext(event.getOrderId(), event)
				.orElse(null);

			if (bookingContext == null) {
				log.warn(
					"[Kafka-Email] 예매확정 이메일스킵 - orderId={}, eventTopic={}, action=skip, reason=order_not_found",
					event.getOrderId(), EventTopic.PAYMENT_COMPLETED);
			} else {
				log.info("[Kafka-Email] 예매확정 이메일발송 - orderId={}, eventTopic={}, action=send", event.getOrderId(),
					EventTopic.PAYMENT_COMPLETED);
				emailService.sendBookingConfirmation(bookingContext);
			}
		} else {
			log.info("[Kafka-Email] 예매확정 이메일스킵 - orderId={}, eventTopic={}, action=skip, reason=order_not_paid",
				event.getOrderId(), EventTopic.PAYMENT_COMPLETED);
		}

		Map<String, Object> paymentContext = emailDataQueryService
			.buildPaymentEmailContext(event.getOrderId(), event)
			.orElse(null);

		if (paymentContext == null) {
			log.warn("[Kafka-Email] 주문없음 이메일스킵 - orderId={}, eventTopic={}, action=skip, reason=order_not_found",
				event.getOrderId(), EventTopic.PAYMENT_COMPLETED);
			return;
		}

		log.info("[Kafka-Email] 결제완료 이메일발송 - orderId={}, eventTopic={}, action=send", event.getOrderId(),
			EventTopic.PAYMENT_COMPLETED);
		emailService.sendPaymentConfirmation(paymentContext);
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
			log.warn("[Kafka-Email] 주문없음 이메일스킵 - orderId={}, eventTopic={}, action=skip, reason=order_not_found",
				event.getOrderId(), EventTopic.ORDER_CANCELLED);
			return;
		}

		log.info("[Kafka-Email] 주문취소 이메일발송 - orderId={}, eventTopic={}, action=send", event.getOrderId(),
			EventTopic.ORDER_CANCELLED);
		emailService.sendCancellationConfirmation(context);
	}
}
