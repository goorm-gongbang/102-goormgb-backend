package com.goormgb.be.ordercore.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Section {
	CENTER_SIDE("중앙 쪽"),
	MIDDLE("중간"),
	CORNER("코너(파울라인)"),
	ANY("무관");

	private final String description;
}
