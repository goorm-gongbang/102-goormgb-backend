package com.goormgb.be.domain.match.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PurchaseStatus {
	PURCHASABLE("구매 가능"),
	NOT_PURCHASABLE("구매 불가");

	private final String description;
}
