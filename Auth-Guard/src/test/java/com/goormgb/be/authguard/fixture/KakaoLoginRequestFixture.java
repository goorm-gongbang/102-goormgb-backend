package com.goormgb.be.authguard.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.authguard.kakao.dto.KakaoLoginRequest;

public final class KakaoLoginRequestFixture {

	public static final String DEFAULT_AUTHORIZATION_CODE = "test_kakao_auth_code_12345";
	public static final String DEFAULT_REDIRECT_URI = "http://localhost:3000/callback/kakao";

	private KakaoLoginRequestFixture() {
	}

	public static KakaoLoginRequest createDefault() {
		return createWithCodeAndRedirectUri(DEFAULT_AUTHORIZATION_CODE, DEFAULT_REDIRECT_URI);
	}

	public static KakaoLoginRequest createWithCode(String authorizationCode) {
		return createWithCodeAndRedirectUri(authorizationCode, DEFAULT_REDIRECT_URI);
	}

	public static KakaoLoginRequest createWithCodeAndRedirectUri(String authorizationCode, String redirectUri) {
		KakaoLoginRequest request = new KakaoLoginRequest();
		ReflectionTestUtils.setField(request, "authorizationCode", authorizationCode);
		ReflectionTestUtils.setField(request, "redirectUri", redirectUri);
		return request;
	}
}
