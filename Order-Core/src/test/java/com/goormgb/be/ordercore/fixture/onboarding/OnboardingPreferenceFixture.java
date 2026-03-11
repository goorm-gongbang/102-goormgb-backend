package com.goormgb.be.ordercore.fixture.onboarding;

import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.onboarding.entity.OnboardingPreference;
import com.goormgb.be.onboarding.enums.EnvironmentPref;
import com.goormgb.be.onboarding.enums.MoodPref;
import com.goormgb.be.onboarding.enums.ObstructionSensitivity;
import com.goormgb.be.onboarding.enums.PriceMode;
import com.goormgb.be.onboarding.enums.SeatHeight;
import com.goormgb.be.onboarding.enums.SeatPositionPref;
import com.goormgb.be.onboarding.enums.Section;
import com.goormgb.be.onboarding.enums.Viewpoint;
import com.goormgb.be.user.entity.User;

public final class OnboardingPreferenceFixture {

	public static final Viewpoint DEFAULT_VIEWPOINT = Viewpoint.CENTER;
	public static final SeatHeight DEFAULT_SEAT_HEIGHT = SeatHeight.LOW;
	public static final Section DEFAULT_SECTION = Section.CENTER_SIDE;

	private OnboardingPreferenceFixture() {
	}

	public static OnboardingPreference createFirst(User user) {
		return OnboardingPreference.builder()
			.user(user)
			.priority(1)
			.viewpoint(Viewpoint.CENTER)
			.seatHeight(SeatHeight.LOW)
			.section(Section.CENTER_SIDE)
			.seatPositionPref(SeatPositionPref.AISLE)
			.environmentPref(EnvironmentPref.SHADE)
			.moodPref(MoodPref.CHEERFUL)
			.obstructionSensitivity(ObstructionSensitivity.NORMAL)
			.priceMode(PriceMode.RANGE)
			.priceMin(30000)
			.priceMax(80000)
			.build();
	}

	public static OnboardingPreference createSecond(User user) {
		return OnboardingPreference.builder()
			.user(user)
			.priority(2)
			.viewpoint(Viewpoint.INFIELD_1B)
			.seatHeight(SeatHeight.MID)
			.section(Section.MIDDLE)
			.seatPositionPref(SeatPositionPref.ANY)
			.environmentPref(EnvironmentPref.SUN_OK)
			.moodPref(MoodPref.QUIET)
			.obstructionSensitivity(ObstructionSensitivity.NET_SENSITIVE)
			.priceMode(PriceMode.ANY)
			.build();
	}

	public static OnboardingPreference createThird(User user) {
		return OnboardingPreference.builder()
			.user(user)
			.priority(3)
			.viewpoint(Viewpoint.OUTFIELD_L)
			.seatHeight(SeatHeight.HIGH)
			.section(Section.CORNER)
			.seatPositionPref(SeatPositionPref.MIDDLE)
			.environmentPref(EnvironmentPref.ANY)
			.moodPref(MoodPref.ANY)
			.obstructionSensitivity(ObstructionSensitivity.RAIL_PILLAR_SENSITIVE)
			.priceMode(PriceMode.RANGE)
			.priceMin(10000)
			.priceMax(50000)
			.build();
	}

	public static OnboardingPreference createWithId(Long id, User user) {
		OnboardingPreference preference = createFirst(user);
		ReflectionTestUtils.setField(preference, "id", id);
		return preference;
	}
}
