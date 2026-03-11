package com.goormgb.be.ordercore.onboarding.dto;

import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.EnvironmentPref;
import com.goormgb.be.domain.onboarding.enums.MoodPref;
import com.goormgb.be.domain.onboarding.enums.ObstructionSensitivity;
import com.goormgb.be.domain.onboarding.enums.PriceMode;
import com.goormgb.be.domain.onboarding.enums.SeatHeight;
import com.goormgb.be.domain.onboarding.enums.SeatPositionPref;
import com.goormgb.be.domain.onboarding.enums.Section;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;

public record OnboardingPreferenceDto(
	Integer priority,
	Long favoriteClubId,
	CheerProximityPref cheerProximityPref,
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
			entity.getFavoriteClub().getId(),
			entity.getCheerProximityPref(),
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