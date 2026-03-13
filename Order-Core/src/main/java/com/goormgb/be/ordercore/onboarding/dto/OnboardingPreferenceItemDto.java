package com.goormgb.be.ordercore.onboarding.dto;

import com.goormgb.be.domain.onboarding.enums.EnvironmentPref;
import com.goormgb.be.domain.onboarding.enums.MoodPref;
import com.goormgb.be.domain.onboarding.enums.ObstructionSensitivity;
import com.goormgb.be.domain.onboarding.enums.PriceMode;
import com.goormgb.be.domain.onboarding.enums.SeatHeight;
import com.goormgb.be.domain.onboarding.enums.SeatPositionPref;
import com.goormgb.be.domain.onboarding.enums.Section;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;

public record OnboardingPreferenceItemDto(
	Integer priority,
	Viewpoint viewpoint,
	SeatHeight seatHeight,
	Section section,
	SeatPositionPref seatPositionPref,
	EnvironmentPref environmentPref,
	MoodPref moodPref,
	ObstructionSensitivity obstructionSensitivity,
	PriceMode priceMode,
	Integer priceMin,
	Integer priceMax
) {
}
