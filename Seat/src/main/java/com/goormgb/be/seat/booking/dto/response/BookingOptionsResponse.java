package com.goormgb.be.seat.booking.dto.response;

public record BookingOptionsResponse(
	Long matchId,
	boolean recommendationEnabled,
	int ticketCount,
	boolean nearAdjacentToggle
) {
}
