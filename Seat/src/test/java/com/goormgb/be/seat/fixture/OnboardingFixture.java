package com.goormgb.be.seat.fixture;

import java.util.Arrays;
import java.util.List;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreferredBlock;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.user.entity.User;

public final class OnboardingFixture {

	private OnboardingFixture() {
	}

	public static OnboardingPreference preference(User user, Club favoriteClub, CheerProximityPref cheerPref) {
		return OnboardingPreference.builder()
			.user(user)
			.favoriteClub(favoriteClub)
			.cheerProximityPref(cheerPref)
			.build();
	}

	public static OnboardingViewpointPriority viewpointPriority(User user, Viewpoint viewpoint, int priority) {
		return OnboardingViewpointPriority.builder()
			.user(user)
			.viewpoint(viewpoint)
			.priority(priority)
			.build();
	}

	public static OnboardingPreferredBlock preferredBlock(User user, Long blockId) {
		return OnboardingPreferredBlock.builder()
			.user(user)
			.blockId(blockId)
			.build();
	}

	public static List<OnboardingPreferredBlock> preferredBlocks(User user, Long... blockIds) {
		return Arrays.stream(blockIds)
			.map(id -> preferredBlock(user, id))
			.toList();
	}
}
