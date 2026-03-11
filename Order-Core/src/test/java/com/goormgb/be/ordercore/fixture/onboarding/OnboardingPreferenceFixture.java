package com.goormgb.be.ordercore.fixture.onboarding;

import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.domain.club.entity.Club;
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
import com.goormgb.be.user.entity.User;

public final class OnboardingPreferenceFixture {

	public static final Viewpoint DEFAULT_VIEWPOINT = Viewpoint.CENTER;
	public static final SeatHeight DEFAULT_SEAT_HEIGHT = SeatHeight.LOW;
	public static final Section DEFAULT_SECTION = Section.CENTER_SIDE;

	private OnboardingPreferenceFixture() {
	}

	public static OnboardingPreference createFirst(User user, Club favoriteClub) {
		return OnboardingPreference.builder()
			.user(user)
			.priority(1)
			.favoriteClub(favoriteClub)
			.cheerProximityPref(CheerProximityPref.NEAR)
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

	public static OnboardingPreference createSecond(User user, Club favoriteClub) {
		return OnboardingPreference.builder()
			.user(user)
			.priority(2)
			.favoriteClub(favoriteClub)
			.cheerProximityPref(CheerProximityPref.ANY)
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

	public static OnboardingPreference createThird(User user, Club favoriteClub) {
		return OnboardingPreference.builder()
			.user(user)
			.priority(3)
			.favoriteClub(favoriteClub)
			.cheerProximityPref(CheerProximityPref.FAR)
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

	public static OnboardingPreference createWithId(Long id, User user, Club favoriteClub) {
		OnboardingPreference preference = createFirst(user, favoriteClub);
		ReflectionTestUtils.setField(preference, "id", id);
		return preference;
	}
}