package com.goormgb.be.authguard.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.authguard.kakao.dto.KakaoLoginRequest;

public final class KakaoLoginRequestFixture {

	public static final String DEFAULT_AUTHORIZATION_CODE = "test_kakao_auth_code_12345";

	private KakaoLoginRequestFixture() {
	}

	public static KakaoLoginRequest createDefault() {
		return createWithCode(DEFAULT_AUTHORIZATION_CODE);
	}

	public static KakaoLoginRequest createWithCode(String authorizationCode) {
		KakaoLoginRequest request = new KakaoLoginRequest();
		ReflectionTestUtils.setField(request, "authorizationCode", authorizationCode);
		return request;
	}
}
