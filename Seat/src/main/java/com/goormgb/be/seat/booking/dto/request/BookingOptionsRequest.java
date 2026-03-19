package com.goormgb.be.seat.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record BookingOptionsRequest(
	boolean recommendationEnabled,
	@Min(1) @Max(10) Integer ticketCount,
	boolean nearAdjacentToggle
) {
}
