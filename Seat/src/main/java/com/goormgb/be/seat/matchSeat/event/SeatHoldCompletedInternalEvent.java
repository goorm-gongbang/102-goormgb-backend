package com.goormgb.be.seat.matchSeat.event;

import java.time.Instant;
import java.util.List;

/**
 * Seat 모듈 내부 이벤트. 트랜잭션 커밋 이후 Kafka 로 전파된다.
 */
public record SeatHoldCompletedInternalEvent(
	Long userId,
	Long matchId,
	List<Long> matchSeatIds,
	Instant occurredAt
) {
}
