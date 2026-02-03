package com.goormgb.be.onboarding.dto.request;

import java.util.List;

public record OnboardingPreferenceCreateRequest(
	MarketingConsent marketingConsent,
	List<PreferenceRequest> preferences
) {
	public record MarketingConsent(Boolean marketingAgreed) {}
}
