package com.goormgb.be.seat.booking.model;

import java.time.Instant;

public record BookingOptions(
	Long userId,
	Long matchId,
	boolean recommendationEnabled,
	Integer ticketCount,
	boolean nearAdjacentToggle,
	Instant createdAt
) {
}
