package com.goormgb.be.ordercore.payment.event;

import java.util.List;

import com.goormgb.be.ordercore.order.entity.Order;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentCompletedInternalEvent {

	private final Order order;
	private final List<Long> matchSeatIds;
	private final String paymentMethod;
}
