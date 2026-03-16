package com.goormgb.be.ordercore.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CashReceiptPurpose {

	PERSONAL_DEDUCTION("개인소득공제"),
	BUSINESS_EXPENSE("사업자지출증빙");

	private final String description;
}
