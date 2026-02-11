package com.goormgb.be.ordercore.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MoodPref {
	CHEERFUL("열정적인 응원"),
	QUIET("조용한 관람"),
	ANY("무관");

	private final String description;
}
