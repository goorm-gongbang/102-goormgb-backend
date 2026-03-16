package com.goormgb.be.ordercore.onboarding.dto.request;

import java.util.List;

import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.ordercore.onboarding.dto.OnboardingPreferenceItemDto;

public record OnboardingPreferenceCreateRequest(
	MarketingConsent marketingConsent,
	Long favoriteClubId,
	CheerProximityPref cheerProximityPref,
	List<Long> preferredBlockIds,
	List<OnboardingPreferenceItemDto> preferences
) {
	public record MarketingConsent(Boolean marketingAgreed) {
	}
}
