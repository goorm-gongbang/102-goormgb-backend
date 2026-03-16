package com.goormgb.be.seat.common.dto.request;

import java.util.List;

public record SeatHoldCreateRequest(
	List<Long> seatIds
) {
}
