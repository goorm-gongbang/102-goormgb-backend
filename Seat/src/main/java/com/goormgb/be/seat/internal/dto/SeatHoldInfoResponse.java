package com.goormgb.be.seat.internal.dto;

import java.time.Instant;

public record SeatHoldInfoResponse(
	Long holdId,
	Long matchSeatId,
	Long userId,
	Instant expiresAt,
	Long sectionId,
	String sectionName,
	Long blockId,
	String blockCode,
	Integer rowNo,
	Integer seatNo
) {
}
