package com.goormgb.be.domain.match.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SaleStatus {
	ON_SALE("예매 가능"),
	UPCOMING("판매 예정"),
	SOLD_OUT("매진"),
	ENDED("예매 마감");

	private final String description;
}
