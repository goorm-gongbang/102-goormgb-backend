package com.goormgb.be.ordercore.email.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Profile({"local", "dev"})
@RestController
@RequestMapping("/test/email")
@RequiredArgsConstructor
public class EmailTestController {

	private final OrderRepository orderRepository;

	@GetMapping("/kafka-message")
	public Map<String, Object> getKafkaTestMessage(
		@RequestParam(required = false) Long orderId
	) {
		Order order;
		if (orderId != null) {
			order = orderRepository.findById(orderId).orElse(null);
			if (order == null) {
				return Map.of("error", "주문 ID " + orderId + "를 찾을 수 없습니다.");
			}
		} else {
			order = orderRepository.findAll().stream().findFirst().orElse(null);
			if (order == null) {
				return Map.of("error", "DB에 주문이 없습니다. 먼저 주문을 생성해주세요.");
			}
		}

		Long id = order.getId();
		String email = order.getOrdererEmail();

		String paymentValue = String.format(
			"{\"orderId\":%d,\"userId\":1,\"matchId\":1,\"matchSeatIds\":[1,2],"
				+ "\"paymentMethod\":\"TOSS_PAY\",\"totalAmount\":30000,"
				+ "\"occurredAt\":\"2026-04-02T08:00:00Z\"}",
			id);

		String cancelValue = String.format(
			"{\"orderId\":%d,\"userId\":1,\"matchId\":1,\"matchSeatIds\":[1,2],"
				+ "\"cancellationFee\":3000,\"refundedAmount\":27000,"
				+ "\"occurredAt\":\"2026-04-02T08:00:00Z\"}",
			id);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("orderId", id);
		result.put("ordererEmail", email);
		result.put("1_payment_completed", Map.of(
			"topic", "payment-completed",
			"value", paymentValue,
			"headers", "{\"__TypeId__\":\"com.goormgb.be.kafka.event.PaymentCompletedEvent\"}"
		));
		result.put("2_order_cancelled", Map.of(
			"topic", "order-cancelled",
			"value", cancelValue,
			"headers", "{\"__TypeId__\":\"com.goormgb.be.kafka.event.OrderCancelledEvent\"}"
		));
		result.put("guide", "Kafka UI(localhost:8090) → 토픽 선택 → Produce Message → Value와 Headers에 위 값 복붙");
		return result;
	}
}
