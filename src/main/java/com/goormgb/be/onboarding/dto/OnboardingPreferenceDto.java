package com.goormgb.be.onboarding.dto;

import com.goormgb.be.onboarding.entity.OnboardingPreference;
import com.goormgb.be.onboarding.enums.EnvironmentPref;
import com.goormgb.be.onboarding.enums.MoodPref;
import com.goormgb.be.onboarding.enums.ObstructionSensitivity;
import com.goormgb.be.onboarding.enums.PriceMode;
import com.goormgb.be.onboarding.enums.SeatHeight;
import com.goormgb.be.onboarding.enums.SeatPositionPref;
import com.goormgb.be.onboarding.enums.Section;
import com.goormgb.be.onboarding.enums.Viewpoint;

public record OnboardingPreferenceDto(
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
	public static OnboardingPreferenceDto from(OnboardingPreference entity) {
		return new OnboardingPreferenceDto(
			entity.getPriority(),
			entity.getViewpoint(),
			entity.getSeatHeight(),
			entity.getSection(),
			entity.getSeatPositionPref(),
			entity.getEnvironmentPref(),
			entity.getMoodPref(),
			entity.getObstructionSensitivity(),
			entity.getPriceMode(),
			entity.getPriceMin(),
			entity.getPriceMax()
		);
	}


}