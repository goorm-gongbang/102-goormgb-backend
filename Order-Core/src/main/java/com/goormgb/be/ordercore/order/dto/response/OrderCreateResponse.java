package com.goormgb.be.ordercore.order.dto.response;

import java.time.Instant;

import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;

public record OrderCreateResponse(
	Long orderId,
	OrderStatus status,
	Long matchId,
	int seatCount,
	int totalAmount,
	int bookingFee,
	Instant createdAt
) {

	public static OrderCreateResponse of(Order order, int seatCount) {
		return new OrderCreateResponse(
			order.getId(),
			order.getStatus(),
			order.getMatch().getId(),
			seatCount,
			order.getTotalAmount(),
			order.getBookingFee(),
			order.getCreatedAt()
		);
	}
}
