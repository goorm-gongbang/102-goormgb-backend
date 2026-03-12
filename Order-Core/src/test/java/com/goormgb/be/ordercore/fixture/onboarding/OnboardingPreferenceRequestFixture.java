package com.goormgb.be.ordercore.fixture.onboarding;

import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.ordercore.onboarding.dto.request.OnboardingPreferenceCreateRequest;
import com.goormgb.be.ordercore.onboarding.dto.request.OnboardingPreferenceUpdateRequest;

public final class OnboardingPreferenceRequestFixture {

	private OnboardingPreferenceRequestFixture() {
	}

	public static OnboardingPreferenceCreateRequest createCreateRequest() {
		return new OnboardingPreferenceCreateRequest(
			new OnboardingPreferenceCreateRequest.MarketingConsent(true),
			1L,
			CheerProximityPref.NEAR,
			OnboardingPreferenceDtoFixture.createPreferredBlockIds(),
			OnboardingPreferenceDtoFixture.createThreePreferences()
		);
	}

	public static OnboardingPreferenceCreateRequest createCreateRequestWithoutMarketing() {
		return new OnboardingPreferenceCreateRequest(
			new OnboardingPreferenceCreateRequest.MarketingConsent(false),
			1L,
			CheerProximityPref.NEAR,
			OnboardingPreferenceDtoFixture.createPreferredBlockIds(),
			OnboardingPreferenceDtoFixture.createTwoPreferences()
		);
	}

	public static OnboardingPreferenceUpdateRequest createUpdateRequest() {
		return new OnboardingPreferenceUpdateRequest(
			2L,
			CheerProximityPref.FAR,
			OnboardingPreferenceDtoFixture.createPreferredBlockIds(),
			OnboardingPreferenceDtoFixture.createTwoPreferences()
		);
	}
}
