package com.goormgb.be.domain.match.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SaleStatus {
	ON_SALE("예매중"),
	UPCOMING("오픈예정"),
	SOLD_OUT("매진"),
	CLOSED("판매종료"),
	ENDED("경기 종료");

	private final String description;
}
