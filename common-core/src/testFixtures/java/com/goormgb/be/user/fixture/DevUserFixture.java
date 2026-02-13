package com.goormgb.be.user.fixture;

import com.goormgb.be.user.entity.DevUser;
import com.goormgb.be.user.entity.User;

import org.springframework.test.util.ReflectionTestUtils;

public final class DevUserFixture {

	public static final String DEFAULT_LOGIN_ID = "devuser";
	public static final String DEFAULT_PASSWORD_HASH = "hashed_password_1234";

	private DevUserFixture() {
	}

	public static DevUser createDefault(User user) {
		return DevUser.builder()
				.loginId(DEFAULT_LOGIN_ID)
				.passwordHash(DEFAULT_PASSWORD_HASH)
				.user(user)
				.build();
	}

	public static DevUser createWithId(Long id, User user) {
		DevUser devUser = createDefault(user);
		ReflectionTestUtils.setField(devUser, "id", id);
		return devUser;
	}

	public static DevUser createWithLoginId(String loginId, User user) {
		return DevUser.builder()
				.loginId(loginId)
				.passwordHash(DEFAULT_PASSWORD_HASH)
				.user(user)
				.build();
	}
}
