package com.goormgb.be.ordercore.onboarding.dto.request;

import java.util.List;

import com.goormgb.be.ordercore.onboarding.dto.OnboardingPreferenceDto;

public record OnboardingPreferenceUpdateRequest(
		List<OnboardingPreferenceDto> preferences
) {
}
