package com.goormgb.be.ordercore.fixture.onboarding;

import java.util.List;

import com.goormgb.be.ordercore.onboarding.dto.OnboardingPreferenceDto;
import com.goormgb.be.ordercore.onboarding.dto.request.OnboardingPreferenceCreateRequest;
import com.goormgb.be.ordercore.onboarding.dto.request.OnboardingPreferenceUpdateRequest;

public final class OnboardingPreferenceRequestFixture {

	private OnboardingPreferenceRequestFixture() {
	}

	public static OnboardingPreferenceCreateRequest createCreateRequest() {
		List<OnboardingPreferenceDto> preferences = List.of(
				OnboardingPreferenceDtoFixture.createFirst(),
				OnboardingPreferenceDtoFixture.createSecond(),
				OnboardingPreferenceDtoFixture.createThird()
		);
		return new OnboardingPreferenceCreateRequest(
				new OnboardingPreferenceCreateRequest.MarketingConsent(true),
				preferences
		);
	}

	public static OnboardingPreferenceCreateRequest createCreateRequestWithoutMarketing() {
		List<OnboardingPreferenceDto> preferences = List.of(
				OnboardingPreferenceDtoFixture.createFirst()
		);
		return new OnboardingPreferenceCreateRequest(
				new OnboardingPreferenceCreateRequest.MarketingConsent(false),
				preferences
		);
	}

	public static OnboardingPreferenceUpdateRequest createUpdateRequest() {
		List<OnboardingPreferenceDto> preferences = List.of(
				OnboardingPreferenceDtoFixture.createFirst(),
				OnboardingPreferenceDtoFixture.createSecond()
		);
		return new OnboardingPreferenceUpdateRequest(preferences);
	}
}
