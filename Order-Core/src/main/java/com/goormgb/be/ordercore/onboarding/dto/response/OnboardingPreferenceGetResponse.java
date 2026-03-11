package com.goormgb.be.ordercore.onboarding.dto.response;

import java.util.List;

import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.ordercore.onboarding.dto.OnboardingPreferenceDto;

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
