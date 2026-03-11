package com.goormgb.be.ordercore.onboarding.dto.response;

import java.time.Instant;

import com.goormgb.be.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "온보딩 완료 여부 조회 응답")
public record OnboardingStatusGetResponse(
	@Schema(description = "온보딩 완료 여부", example = "true")
	Boolean onboardingStatus,
	@Schema(description = "온보딩 완료 시각 (UTC ISO-8601), 미완료 시 null", type = "string", example = "2026-03-10T10:00:00Z", nullable = true)
	Instant onboardingCompletedAt
) {
	public static OnboardingStatusGetResponse from(User user) {
		return new OnboardingStatusGetResponse(
			user.getOnboardingCompleted(),
			user.getOnboardingCompletedAt()
		);
	}
}
