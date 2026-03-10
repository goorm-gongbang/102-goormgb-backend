package com.goormgb.be.ordercore.onboarding.dto.response;

import java.time.Instant;

import com.goormgb.be.user.entity.User;

public record OnboardingPreferenceCreateResponse(
		boolean onboardingStatus,
		Instant onboardingCompletedAt,
		boolean marketingConsent,
		Instant marketingConsentedAt
) {
	public static OnboardingPreferenceCreateResponse from(User user) {
		return new OnboardingPreferenceCreateResponse(
				Boolean.TRUE.equals(user.getOnboardingCompleted()),
				user.getOnboardingCompletedAt(),
				Boolean.TRUE.equals(user.getMarketingConsent()),
				user.getMarketingConsentedAt()
		);
	}
}
