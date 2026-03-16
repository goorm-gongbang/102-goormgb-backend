package com.goormgb.be.ordercore.onboarding.dto.response;

import java.util.List;

import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreferredBlock;
import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.ordercore.onboarding.dto.OnboardingPreferenceItemDto;

public record OnboardingPreferenceGetResponse(
	Long favoriteClubId,
	String favoriteClubName,
	CheerProximityPref cheerProximityPref,
	List<Long> preferredBlockIds,
	List<OnboardingPreferenceItemDto> preferences
) {
	public static OnboardingPreferenceGetResponse from(
		OnboardingPreference preference,
		List<OnboardingViewpointPriority> viewpointPriorities,
		List<OnboardingPreferredBlock> preferredBlocks
	) {
		List<OnboardingPreferenceItemDto> items = viewpointPriorities.stream()
			.map(vp -> new OnboardingPreferenceItemDto(
				vp.getPriority(),
				vp.getViewpoint(),
				preference.getSeatHeight(),
				preference.getSection(),
				preference.getSeatPositionPref(),
				preference.getEnvironmentPref(),
				preference.getMoodPref(),
				preference.getObstructionSensitivity(),
				preference.getPriceMode(),
				preference.getPriceMin(),
				preference.getPriceMax()
			))
			.toList();

		return new OnboardingPreferenceGetResponse(
			preference.getFavoriteClub().getId(),
			preference.getFavoriteClub().getKoName(),
			preference.getCheerProximityPref(),
			preferredBlocks.stream()
				.map(OnboardingPreferredBlock::getBlockId)
				.toList(),
			items
		);
	}
}
