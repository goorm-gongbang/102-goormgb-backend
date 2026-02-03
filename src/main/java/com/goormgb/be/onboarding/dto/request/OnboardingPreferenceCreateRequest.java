package com.goormgb.be.onboarding.dto.request;

import java.util.List;

import com.goormgb.be.onboarding.enums.EnvironmentPref;
import com.goormgb.be.onboarding.enums.MoodPref;
import com.goormgb.be.onboarding.enums.ObstructionSensitivity;
import com.goormgb.be.onboarding.enums.PriceMode;
import com.goormgb.be.onboarding.enums.SeatHeight;
import com.goormgb.be.onboarding.enums.SeatPositionPref;
import com.goormgb.be.onboarding.enums.Section;
import com.goormgb.be.onboarding.enums.Viewpoint;

public record OnboardingPreferenceCreateRequest(
	MarketingConsent marketingConsent,
	List<Preference> preferences
) {
	public record MarketingConsent(Boolean marketingAgreed) {}

	public record Preference(
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
	) {}
}
