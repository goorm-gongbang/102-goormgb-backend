package com.goormgb.be.ordercore.metrics.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethodType {
	KAKAOPAY("kakaopay"),
	TOSSPAY("tosspay"),
	BANK_TRANSFER("bank_transfer");

	private final String value;
}