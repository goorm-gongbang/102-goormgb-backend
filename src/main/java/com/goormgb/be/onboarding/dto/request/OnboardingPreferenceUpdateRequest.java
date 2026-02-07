package com.goormgb.be.onboarding.dto.request;

import java.util.List;

import com.goormgb.be.onboarding.dto.OnboardingPreferenceDto;

public record OnboardingPreferenceUpdateRequest(
	List<OnboardingPreferenceDto> preferences
) {
}
