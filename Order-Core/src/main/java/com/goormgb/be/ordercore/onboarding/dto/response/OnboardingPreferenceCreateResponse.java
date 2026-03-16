package com.goormgb.be.ordercore.onboarding.dto.response;

import java.time.Instant;

import com.goormgb.be.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "온보딩 선호도 생성 응답")
public record OnboardingPreferenceCreateResponse(
		@Schema(description = "온보딩 완료 여부", example = "true")
		boolean onboardingStatus,
		@Schema(description = "온보딩 완료 시각 (UTC ISO-8601)", type = "string", example = "2026-03-10T10:00:00Z")
		Instant onboardingCompletedAt,
		@Schema(description = "마케팅 수신 동의 여부", example = "true")
		boolean marketingConsent,
		@Schema(description = "마케팅 수신 동의 시각 (UTC ISO-8601), 미동의 시 null", type = "string", example = "2026-03-10T10:00:00Z", nullable = true)
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
