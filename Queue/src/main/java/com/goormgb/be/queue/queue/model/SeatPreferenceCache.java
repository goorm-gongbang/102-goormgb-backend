package com.goormgb.be.queue.queue.model;

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
