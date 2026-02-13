package com.goormgb.be.user.fixture;

import com.goormgb.be.user.entity.User;

import org.springframework.test.util.ReflectionTestUtils;

public final class UserFixture {

	public static final String DEFAULT_EMAIL = "test@example.com";
	public static final String DEFAULT_NICKNAME = "테스트유저";

	private UserFixture() {
	}

	public static User createDefault() {
		return User.builder()
				.email(DEFAULT_EMAIL)
				.nickname(DEFAULT_NICKNAME)
				.build();
	}

	public static User createWithId(Long id) {
		User user = createDefault();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	public static User createDeactivated() {
		User user = createDefault();
		user.deactivate();
		return user;
	}

	public static User createOnboardingCompleted() {
		User user = createDefault();
		user.completeOnboarding();
		return user;
	}
}
