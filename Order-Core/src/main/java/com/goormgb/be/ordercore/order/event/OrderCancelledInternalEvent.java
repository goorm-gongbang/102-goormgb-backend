package com.goormgb.be.ordercore.order.event;

import java.util.List;

import com.goormgb.be.ordercore.order.entity.Order;

public record OrderCancelledInternalEvent(Order order, List<Long> matchSeatIds) {
}
