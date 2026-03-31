package com.goormgb.be.ordercore.payment.event;

import java.util.List;

import com.goormgb.be.ordercore.order.entity.Order;

public record PaymentCompletedInternalEvent(
		Order order,
		List<Long> matchSeatIds,
		String paymentMethod) {
}
