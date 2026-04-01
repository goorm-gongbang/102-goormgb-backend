package com.goormgb.be.ordercore.payment.event;

import java.util.List;

public record BankTransferExpiredInternalEvent(
		Long orderId,
		Long userId,
		Long matchId,
		Long paymentId,
		List<Long> matchSeatIds) {
}
