package com.goormgb.be.ordercore.onboarding.dto.response;

import java.util.List;

import com.goormgb.be.ordercore.onboarding.dto.OnboardingPreferenceDto;
import com.goormgb.be.ordercore.onboarding.entity.OnboardingPreference;

public record OnboardingPreferenceGetResponse(
		List<OnboardingPreferenceDto> preferences
) {
	public static OnboardingPreferenceGetResponse from(List<OnboardingPreference> entities) {
		List<OnboardingPreferenceDto> preferences = entities.stream()
				.map(OnboardingPreferenceDto::from)
				.toList();

		return new OnboardingPreferenceGetResponse(preferences);
	}
}
