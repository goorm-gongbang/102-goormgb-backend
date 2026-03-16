package com.goormgb.be.global.model;

import java.time.Instant;
import java.util.List;

public record SeatPreferenceCache(
	Long userId,
	Long matchId,
	boolean recommendationEnabled,
	int ticketCount,
	List<Long> preferredBlockIds,
	Instant enteredAt
) {
}
