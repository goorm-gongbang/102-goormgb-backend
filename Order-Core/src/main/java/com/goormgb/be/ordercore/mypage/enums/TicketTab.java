package com.goormgb.be.ordercore.mypage.enums;

import java.util.List;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.order.enums.OrderStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketTab {

	BOOKED(List.of(
		OrderStatus.PAYMENT_PENDING,
		OrderStatus.PAID,
		OrderStatus.CANCEL_REQUESTED,
		OrderStatus.REFUND_PROCESSING
	)),
	CANCEL_REFUND(List.of(
		OrderStatus.CANCELLED,
		OrderStatus.REFUND_COMPLETED
	));

	private final List<OrderStatus> statuses;

	public static TicketTab fromString(String value) {
		try {
			return TicketTab.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new CustomException(ErrorCode.INVALID_TICKET_TAB);
		}
	}

	public List<String> getStatusNames() {
		return statuses.stream().map(Enum::name).toList();
	}
}
