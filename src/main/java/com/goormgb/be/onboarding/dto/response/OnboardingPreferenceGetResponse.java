package com.goormgb.be.onboarding.dto.response;

import java.util.List;

import com.goormgb.be.onboarding.dto.OnboardingPreferenceDto;

public record OnboardingPreferenceGetResponse (
	List<OnboardingPreferenceDto> preferences
) {
	public static OnboardingPreferenceGetResponse from(List<OnboardingPreferenceDto> preferences){
		return new OnboardingPreferenceGetResponse(preferences);
	}
}
