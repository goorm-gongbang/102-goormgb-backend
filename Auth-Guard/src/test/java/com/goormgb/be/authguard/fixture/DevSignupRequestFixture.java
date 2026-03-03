package com.goormgb.be.authguard.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.authguard.auth.dto.DevSignupRequest;

public final class DevSignupRequestFixture {

	public static final String DEFAULT_LOGIN_ID = "testdev";
	public static final String DEFAULT_PASSWORD = "password1234";
	public static final String DEFAULT_NICKNAME = "테스트개발자";
	public static final String DEFAULT_EMAIL = "dev@example.com";

	private DevSignupRequestFixture() {
	}

	private static DevSignupRequest create(String loginId, String password, String nickname, String email) {
		DevSignupRequest request = new DevSignupRequest();
		ReflectionTestUtils.setField(request, "loginId", loginId);
		ReflectionTestUtils.setField(request, "password", password);
		ReflectionTestUtils.setField(request, "nickname", nickname);
		ReflectionTestUtils.setField(request, "email", email);
		return request;
	}

	public static DevSignupRequest createDefault() {
		return create(DEFAULT_LOGIN_ID, DEFAULT_PASSWORD, DEFAULT_NICKNAME, DEFAULT_EMAIL);
	}

	public static DevSignupRequest createWithLoginId(String loginId) {
		return create(loginId, DEFAULT_PASSWORD, DEFAULT_NICKNAME, DEFAULT_EMAIL);
	}
}
