package com.goormgb.be.ordercore.order.dto.request;

import com.goormgb.be.domain.ticket.enums.TicketType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "좌석별 주문 항목")
public record SeatOrderItem(

	@Schema(description = "매치 좌석 ID (주문서 조회에서 받은 matchSeatId)", example = "149801", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "matchSeatId는 필수입니다.")
	Long matchSeatId,

	@Schema(description = "티켓 타입 (ADULT, YOUTH, MILITARY, CHILD, SENIOR, VETERAN, DISABLED)", example = "ADULT", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "ticketType은 필수입니다.")
	TicketType ticketType
) {
}
