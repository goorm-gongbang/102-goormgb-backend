package com.goormgb.be.ordercore.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {

	VIRTUAL_ACCOUNT("무통장 입금"),
	TOSS_PAY("토스페이"),
	KAKAO_PAY("카카오페이");

	private final String description;
}
