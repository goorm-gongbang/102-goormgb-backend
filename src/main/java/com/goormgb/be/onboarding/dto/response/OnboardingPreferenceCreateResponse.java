package com.goormgb.be.onboarding.dto.response;

import java.time.LocalDateTime;

import com.goormgb.be.user.entity.User;

public record OnboardingPreferenceCreateResponse (
	boolean onboardingStatus,
	LocalDateTime onboardingCompletedAt,
	boolean marketingConsent,
	LocalDateTime marketingConsentedAt
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
