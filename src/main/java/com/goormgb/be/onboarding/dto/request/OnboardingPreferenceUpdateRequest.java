package com.goormgb.be.onboarding.dto.request;

import java.util.List;

public record OnboardingPreferenceUpdateRequest(
	List<PreferenceRequest> preferences
) {
}
