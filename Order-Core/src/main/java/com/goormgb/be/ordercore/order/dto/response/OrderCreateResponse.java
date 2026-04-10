package com.goormgb.be.ordercore.order.dto.response;

import java.time.Instant;

import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 생성 응답")
public record OrderCreateResponse(
	@Schema(description = "주문 ID", example = "1") Long orderId,
	@Schema(description = "주문 상태", example = "PAYMENT_PENDING") OrderStatus status,
	@Schema(description = "경기 ID", example = "1") Long matchId,
	@Schema(description = "주문 좌석 수", example = "2") int seatCount,
	@Schema(description = "총 결제 금액 (원)", example = "42000") int totalAmount,
	@Schema(description = "예매 수수료 (원)", example = "2000") int bookingFee,
	@Schema(description = "주문 생성 일시 (UTC)", example = "2026-03-22T05:39:00Z") Instant createdAt
) {

	public static OrderCreateResponse of(Order order, int seatCount) {
		return new OrderCreateResponse(
			order.getId(),
			order.getStatus(),
			order.getMatchId(),
			seatCount,
			order.getTotalAmount(),
			order.getBookingFee(),
			order.getCreatedAt()
		);
	}
}
