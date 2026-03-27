package com.goormgb.be.ordercore.mypage.dto.response;

import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;

public record MyPageTicketCancelResponse(
	Long ticketId,
	OrderStatus status,
	int totalAmount,
	int cancellationFee,
	int refundedAmount
) {
	public static MyPageTicketCancelResponse of(Order order) {
		return new MyPageTicketCancelResponse(
			order.getId(),
			order.getStatus(),
			order.getTotalAmount(),
			order.getCancellationFee(),
			order.getRefundedAmount()
		);
	}
}
