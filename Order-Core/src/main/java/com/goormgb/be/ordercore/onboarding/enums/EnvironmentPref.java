package com.goormgb.be.ordercore.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnvironmentPref {
	SHADE("그늘 선호"),
	SUN_OK("햇빛 무관"),
	ANY("무관");

	private final String description;
}
