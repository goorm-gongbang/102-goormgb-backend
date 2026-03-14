package com.goormgb.be.ordercore.order.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {

	PAYMENT_PENDING("입금 대기"),
	PAID("결제 완료"),
	CANCEL_REQUESTED("취소 요청"),
	CANCELLED("취소 완료"),
	REFUND_PROCESSING("환불 처리 중"),
	REFUND_COMPLETED("환불 완료");

	private final String description;
}
