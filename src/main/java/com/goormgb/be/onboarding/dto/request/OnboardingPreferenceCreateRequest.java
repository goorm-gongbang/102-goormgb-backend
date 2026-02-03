package com.goormgb.be.onboarding.dto.request;

import java.util.List;

import com.goormgb.be.onboarding.dto.OnboardingPreferenceDto;

public record OnboardingPreferenceCreateRequest(
	MarketingConsent marketingConsent,
	List<OnboardingPreferenceDto> preferences
) {
	public record MarketingConsent(Boolean marketingAgreed) {}
}
