package com.goormgb.be.ordercore.order.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.UserBlockedEvent;
import com.goormgb.be.ordercore.order.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBlockedEventConsumer {

	private final OrderService orderService;

	/**
	 * 유저 차단 이벤트를 수신하여 해당 유저의 주문 상태를 UNDER_REVIEW로 변경한다.
	 */
	@KafkaListener(
			topics = EventTopic.USER_BLOCKED,
			groupId = "${spring.kafka.consumer.group-id}"
	)
	public void handleUserBlocked(UserBlockedEvent event) {
		log.info("[Kafka] 유저 차단 이벤트 수신 - userId={}, occurredAt={}", event.getUserId(), event.getOccurredAt());

		int updatedCount = orderService.markOrdersUnderReviewByBlockedUser(event.getUserId());

		log.warn("[Kafka] 유저 차단 주문 상태 변경 완료 - userId={}, updatedCount={}", event.getUserId(), updatedCount);
	}
}
