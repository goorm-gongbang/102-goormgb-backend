package com.goormgb.be.ordercore.order.event;

import java.util.List;

public record OrderCancelledInternalEvent(
		Long orderId,
		Long userId,
		Long matchId,
		Integer cancellationFee,
		Integer refundedAmount,
		List<Long> matchSeatIds) {
}
