package com.goormgb.be.seat.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record BookingOptionsRequest(
	boolean recommendationEnabled,
	@Min(value = 1, message = "1 이상이어야 합니다")
	@Max(value = 8, message = "8 이하여야 합니다")
	Integer ticketCount,
	boolean nearAdjacentToggle
) {
}
