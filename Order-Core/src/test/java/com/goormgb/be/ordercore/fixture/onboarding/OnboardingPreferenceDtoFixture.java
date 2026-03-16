package com.goormgb.be.ordercore.fixture.onboarding;

import java.util.List;

import com.goormgb.be.domain.onboarding.enums.EnvironmentPref;
import com.goormgb.be.domain.onboarding.enums.MoodPref;
import com.goormgb.be.domain.onboarding.enums.ObstructionSensitivity;
import com.goormgb.be.domain.onboarding.enums.PriceMode;
import com.goormgb.be.domain.onboarding.enums.SeatHeight;
import com.goormgb.be.domain.onboarding.enums.SeatPositionPref;
import com.goormgb.be.domain.onboarding.enums.Section;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.ordercore.onboarding.dto.OnboardingPreferenceItemDto;

public final class OnboardingPreferenceDtoFixture {

	private OnboardingPreferenceDtoFixture() {
	}

	public static List<OnboardingPreferenceItemDto> createThreePreferences() {
		return List.of(
			new OnboardingPreferenceItemDto(
				1, Viewpoint.CENTER,
				SeatHeight.LOW, Section.CENTER_SIDE,
				SeatPositionPref.AISLE, EnvironmentPref.SHADE,
				MoodPref.CHEERFUL, ObstructionSensitivity.NORMAL,
				PriceMode.RANGE, 30000, 80000
			),
			new OnboardingPreferenceItemDto(
				2, Viewpoint.INFIELD_1B,
				null, null, null, null, null, null, null, null, null
			),
			new OnboardingPreferenceItemDto(
				3, Viewpoint.OUTFIELD_L,
				null, null, null, null, null, null, null, null, null
			)
		);
	}

	public static List<OnboardingPreferenceItemDto> createTwoPreferences() {
		return List.of(
			new OnboardingPreferenceItemDto(
				1, Viewpoint.CENTER,
				null, null, null, null, null, null, PriceMode.ANY, null, null
			),
			new OnboardingPreferenceItemDto(
				2, Viewpoint.INFIELD_1B,
				null, null, null, null, null, null, null, null, null
			)
		);
	}

	public static List<Long> createPreferredBlockIds() {
		return List.of(1L, 5L, 12L, 23L, 45L);
	}
}
