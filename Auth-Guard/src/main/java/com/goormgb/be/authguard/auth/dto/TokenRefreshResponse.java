package com.goormgb.be.authguard.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "토큰 응답")
@Getter
@Builder
public class TokenRefreshResponse {

	@Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
	private String accessToken;
	@Schema(description = "이용약관 동의 필요 여부", example = "false")
	private Boolean agreementRequired;
	@Schema(description = "온보딩 진행 필요 여부", example = "true")
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
