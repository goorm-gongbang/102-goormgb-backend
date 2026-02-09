package com.goormgb.be.user.dto.response;

import com.goormgb.be.user.entity.User;

import java.time.LocalDateTime;

public record OnboardingStatusResponse(
        boolean onboardingStatus,
        LocalDateTime onboardingCompletedAt
) {
    public static OnboardingStatusResponse from(User user) {
        return new OnboardingStatusResponse(
                Boolean.TRUE.equals(user.getOnboardingCompleted()),
                user.getOnboardingCompletedAt()
        );
    }
}
