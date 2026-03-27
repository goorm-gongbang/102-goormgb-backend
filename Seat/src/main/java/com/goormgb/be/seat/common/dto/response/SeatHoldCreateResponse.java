package com.goormgb.be.seat.common.dto.response;

import java.time.Instant;
import java.util.List;

public record SeatHoldCreateResponse(
	Long matchId,
	List<Long> matchSeatIds,
	int seatCount,
	Instant holdExpiresAt
) {

	public static SeatHoldCreateResponse of(Long matchId, List<Long> matchSeatIds, Instant holdExpiresAt) {
		return new SeatHoldCreateResponse(matchId, matchSeatIds, matchSeatIds.size(), holdExpiresAt);
	}
}
