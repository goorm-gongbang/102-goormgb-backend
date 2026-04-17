package com.goormgb.be.seat.matchSeat.event;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.SeatHoldCompletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 좌석 홀드 성공 시점에 Queue 모듈로 READY 슬롯 회수 이벤트를 발행한다.
 *
 * <p>내부 Spring 이벤트를 {@code AFTER_COMMIT} 시점에 Kafka 로 전파하는 구조로,
 * 트랜잭션이 롤백된 경우에는 이벤트가 발행되지 않도록 보장한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldEventPublisher {

	private final ApplicationEventPublisher applicationEventPublisher;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public void publishSeatHoldCompleted(Long userId, Long matchId, List<Long> matchSeatIds) {
		applicationEventPublisher.publishEvent(
			new SeatHoldCompletedInternalEvent(userId, matchId, matchSeatIds, Instant.now())
		);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSeatHoldCompleted(SeatHoldCompletedInternalEvent internalEvent) {
		SeatHoldCompletedEvent event = SeatHoldCompletedEvent.builder()
			.userId(internalEvent.userId())
			.matchId(internalEvent.matchId())
			.matchSeatIds(internalEvent.matchSeatIds())
			.occurredAt(internalEvent.occurredAt())
			.build();

		String key = internalEvent.matchId() + ":" + internalEvent.userId();

		kafkaTemplate.send(EventTopic.SEAT_HOLD_COMPLETED, key, event)
			.whenComplete((SendResult<String, Object> result, Throwable ex) -> {
				if (ex != null) {
					log.error("[Kafka] 좌석 홀드 완료 이벤트 발행 실패: userId={}, matchId={}, error={}",
						internalEvent.userId(), internalEvent.matchId(), ex.getMessage(), ex);
				} else {
					log.info(
						"[Kafka] 좌석 홀드 완료 이벤트 발행 성공: userId={}, matchId={}, seatCount={}, offset={}",
						internalEvent.userId(),
						internalEvent.matchId(),
						internalEvent.matchSeatIds().size(),
						result.getRecordMetadata().offset()
					);
				}
			});
	}
}
