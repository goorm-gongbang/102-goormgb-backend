package com.goormgb.be.queue.queue.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.PaymentCompletedEvent;
import com.goormgb.be.kafka.event.SeatHoldCompletedEvent;
import com.goormgb.be.queue.queue.repository.QueueRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 좌석 홀드 성공·결제 완료 시 Queue 의 READY 슬롯을 즉시 회수한다.
 *
 * <p>기존 구조에서는 READY TTL(60초) 만료까지 슬롯이 유지돼 뒤에서 대기하던 사용자가
 * 실시간으로 승급되지 못했다. 본 컨슈머가 terminal 이벤트 수신 시점에
 * {@code leaveQueueAtomic} 을 호출해 WAITING/READY/expired marker/active match 를 한 번에
 * 정리하므로, 다음 {@code QueuePromotionScheduler} 주기에서 신규 사용자가 즉시 승급된다.</p>
 *
 * <p>Lua 스크립트가 모든 Redis 연산을 idempotent 하게 수행하므로 Kafka 재전송에도 안전하다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueSlotReleaseConsumer {

	private final QueueRedisRepository queueRedisRepository;

	@KafkaListener(topics = EventTopic.PAYMENT_COMPLETED, groupId = "queue-service")
	public void handlePaymentCompleted(PaymentCompletedEvent event) {
		releaseReadySlot(event.getMatchId(), event.getUserId(), "payment-completed", event.getOrderId());
	}

	@KafkaListener(topics = EventTopic.SEAT_HOLD_COMPLETED, groupId = "queue-service")
	public void handleSeatHoldCompleted(SeatHoldCompletedEvent event) {
		releaseReadySlot(event.getMatchId(), event.getUserId(), "seat-hold-completed", null);
	}

	private void releaseReadySlot(Long matchId, Long userId, String source, Long orderId) {
		if (matchId == null || userId == null) {
			log.warn("[Kafka] READY 슬롯 회수 스킵 - source={}, reason=missing_ids, matchId={}, userId={}",
				source, matchId, userId);
			return;
		}

		try {
			queueRedisRepository.leaveQueueAtomic(matchId, userId);
			log.info("[Kafka] READY 슬롯 회수 완료 - source={}, matchId={}, userId={}, orderId={}",
				source, matchId, userId, orderId);
		} catch (Exception e) {
			// DLT/재시도는 CommonErrorHandler 가 담당한다.
			log.error("[Kafka] READY 슬롯 회수 실패 - source={}, matchId={}, userId={}, error={}",
				source, matchId, userId, e.getMessage(), e);
			throw e;
		}
	}
}
