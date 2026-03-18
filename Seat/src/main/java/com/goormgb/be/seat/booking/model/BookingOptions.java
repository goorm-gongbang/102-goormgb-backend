package com.goormgb.be.seat.booking.model;

import java.time.Instant;

public record BookingOptions(
	Long userId,
	Long matchId,
	boolean recommendationEnabled,
	int ticketCount,
	boolean nearAdjacentToggle,
	Instant createdAt
) {
}
