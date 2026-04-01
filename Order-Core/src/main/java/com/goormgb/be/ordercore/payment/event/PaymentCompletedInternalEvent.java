package com.goormgb.be.ordercore.payment.event;

import java.util.List;

public record PaymentCompletedInternalEvent(
		Long orderId,
		Long userId,
		Long matchId,
		List<Long> matchSeatIds,
		Integer totalAmount,
		String paymentMethod) {
}
