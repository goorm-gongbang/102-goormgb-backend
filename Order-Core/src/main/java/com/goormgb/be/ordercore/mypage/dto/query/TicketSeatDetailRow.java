package com.goormgb.be.ordercore.mypage.dto.query;

import com.goormgb.be.domain.ticket.enums.TicketType;

public record TicketSeatDetailRow(
	String sectionName,
	String blockCode,
	int rowNo,
	int seatNo,
	int price,
	TicketType ticketType
) {
}