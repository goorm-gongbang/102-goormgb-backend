package com.goormgb.be.authguard.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenRefreshResponse {

	private String accessToken;

	public static TokenRefreshResponse of(String accessToken) {
		return new TokenRefreshResponse(accessToken);
	}
}