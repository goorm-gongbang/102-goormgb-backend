package com.goormgb.be.ordercore.order.dto.request;

import com.goormgb.be.domain.ticket.enums.TicketType;

import jakarta.validation.constraints.NotNull;

public record SeatOrderItem(

	@NotNull(message = "matchSeatId는 필수입니다.")
	Long matchSeatId,

	@NotNull(message = "ticketType은 필수입니다.")
	TicketType ticketType
) {
}
