package com.goormgb.be.authguard.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.authguard.auth.dto.DevLoginRequest;

public final class DevLoginRequestFixture {

	public static final String DEFAULT_LOGIN_ID = "dev";
	public static final String DEFAULT_PASSWORD = "1234";

	private DevLoginRequestFixture() {
	}

	public static DevLoginRequest createDefault() {
		DevLoginRequest request = new DevLoginRequest();
		ReflectionTestUtils.setField(request, "loginId", DEFAULT_LOGIN_ID);
		ReflectionTestUtils.setField(request, "password", DEFAULT_PASSWORD);
		return request;
	}

	public static DevLoginRequest createWithCredentials(String loginId, String password) {
		DevLoginRequest request = new DevLoginRequest();
		ReflectionTestUtils.setField(request, "loginId", loginId);
		ReflectionTestUtils.setField(request, "password", password);
		return request;
	}
}
