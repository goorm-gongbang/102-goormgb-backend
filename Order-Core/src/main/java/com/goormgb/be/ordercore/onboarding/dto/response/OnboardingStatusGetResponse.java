package com.goormgb.be.ordercore.onboarding.dto.response;

import java.time.LocalDateTime;

import com.goormgb.be.user.entity.User;

public record OnboardingStatusGetResponse(
	Boolean onboardingStatus,
	LocalDateTime onboardingCompletedAt
) {
	public static OnboardingStatusGetResponse from(User user) {
		return new OnboardingStatusGetResponse(
			user.getOnboardingCompleted(),
			user.getOnboardingCompletedAt()
		);
	}
}
