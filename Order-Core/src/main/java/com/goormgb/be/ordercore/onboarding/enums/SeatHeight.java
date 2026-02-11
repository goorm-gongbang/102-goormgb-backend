package com.goormgb.be.ordercore.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatHeight {
	LOW("하단"),
	MID("중단"),
	HIGH("상단"),
	ANY("무관");

	private final String description;
}
