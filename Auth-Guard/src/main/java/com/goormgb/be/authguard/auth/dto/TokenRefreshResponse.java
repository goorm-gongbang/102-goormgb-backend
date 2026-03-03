package com.goormgb.be.authguard.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenRefreshResponse {

	private String accessToken;
	private Boolean agreementRequired;
	private Boolean onboardingRequired;

	public static TokenRefreshResponse of(String accessToken) {
		return TokenRefreshResponse.builder()
				.accessToken(accessToken)
				.build();
	}

	public static TokenRefreshResponse of(String accessToken, boolean agreementRequired, boolean onboardingRequired) {
		return TokenRefreshResponse.builder()
				.accessToken(accessToken)
				.agreementRequired(agreementRequired)
				.onboardingRequired(onboardingRequired)
				.build();
	}
}
