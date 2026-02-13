package com.goormgb.be.authguard.fixture;

import com.goormgb.be.authguard.auth.dto.DevSignupRequest;

import org.springframework.test.util.ReflectionTestUtils;

public final class DevSignupRequestFixture {

	public static final String DEFAULT_LOGIN_ID = "testdev";
	public static final String DEFAULT_PASSWORD = "password1234";
	public static final String DEFAULT_NICKNAME = "테스트개발자";
	public static final String DEFAULT_EMAIL = "dev@example.com";

	private DevSignupRequestFixture() {
	}

	public static DevSignupRequest createDefault() {
		DevSignupRequest request = new DevSignupRequest();
		ReflectionTestUtils.setField(request, "loginId", DEFAULT_LOGIN_ID);
		ReflectionTestUtils.setField(request, "password", DEFAULT_PASSWORD);
		ReflectionTestUtils.setField(request, "nickname", DEFAULT_NICKNAME);
		ReflectionTestUtils.setField(request, "email", DEFAULT_EMAIL);
		return request;
	}

	public static DevSignupRequest createWithLoginId(String loginId) {
		DevSignupRequest request = createDefault();
		ReflectionTestUtils.setField(request, "loginId", loginId);
		return request;
	}
}
