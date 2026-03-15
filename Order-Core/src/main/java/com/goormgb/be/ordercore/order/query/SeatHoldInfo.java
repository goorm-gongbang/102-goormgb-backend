package com.goormgb.be.ordercore.order.query;

import java.time.Instant;

public record SeatHoldInfo(
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
	public boolean isExpired(Instant now) {
		return expiresAt.isBefore(now);
	}
}
