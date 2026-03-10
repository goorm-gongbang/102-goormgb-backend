package com.goormgb.be.ordercore.onboarding.dto.response;

import java.time.Instant;

import com.goormgb.be.user.entity.User;

public record OnboardingStatusGetResponse(
	Boolean onboardingStatus,
	Instant onboardingCompletedAt
) {
	public static OnboardingStatusGetResponse from(User user) {
		return new OnboardingStatusGetResponse(
			user.getOnboardingCompleted(),
			user.getOnboardingCompletedAt()
		);
	}
}
