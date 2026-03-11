package com.goormgb.be.ordercore.fixture.onboarding;

import com.goormgb.be.domain.onboarding.enums.EnvironmentPref;
import com.goormgb.be.domain.onboarding.enums.MoodPref;
import com.goormgb.be.domain.onboarding.enums.ObstructionSensitivity;
import com.goormgb.be.domain.onboarding.enums.PriceMode;
import com.goormgb.be.domain.onboarding.enums.SeatHeight;
import com.goormgb.be.domain.onboarding.enums.SeatPositionPref;
import com.goormgb.be.domain.onboarding.enums.Section;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.ordercore.onboarding.dto.OnboardingPreferenceDto;

public final class OnboardingPreferenceDtoFixture {

	private OnboardingPreferenceDtoFixture() {
	}

	public static OnboardingPreferenceDto createFirst() {
		return new OnboardingPreferenceDto(
			1,
			Viewpoint.CENTER,
			SeatHeight.LOW,
			Section.CENTER_SIDE,
			SeatPositionPref.AISLE,
			EnvironmentPref.SHADE,
			MoodPref.CHEERFUL,
			ObstructionSensitivity.NORMAL,
			PriceMode.RANGE,
			30000,
			80000
		);
	}

	public static OnboardingPreferenceDto createSecond() {
		return new OnboardingPreferenceDto(
			2,
			Viewpoint.INFIELD_1B,
			SeatHeight.MID,
			Section.MIDDLE,
			SeatPositionPref.ANY,
			EnvironmentPref.SUN_OK,
			MoodPref.QUIET,
			ObstructionSensitivity.NET_SENSITIVE,
			PriceMode.ANY,
			null,
			null
		);
	}

	public static OnboardingPreferenceDto createThird() {
		return new OnboardingPreferenceDto(
			3,
			Viewpoint.OUTFIELD_L,
			SeatHeight.HIGH,
			Section.CORNER,
			SeatPositionPref.MIDDLE,
			EnvironmentPref.ANY,
			MoodPref.ANY,
			ObstructionSensitivity.RAIL_PILLAR_SENSITIVE,
			PriceMode.RANGE,
			10000,
			50000
		);
	}
}
