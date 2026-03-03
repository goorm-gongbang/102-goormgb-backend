package com.goormgb.be.user.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.user.entity.User;

public final class UserFixture {

	public static final String DEFAULT_EMAIL = "test@example.com";
	public static final String DEFAULT_NICKNAME = "테스트유저";
	public static final String DEFAULT_PROFILE_IMAGE_URL = "https://example.com/profile.jpg";

	private UserFixture() {
	}

	private static User.UserBuilder defaultBuilder() {
		return User.builder()
				.email(DEFAULT_EMAIL)
				.nickname(DEFAULT_NICKNAME);
	}

	public static User createDefault() {
		return defaultBuilder()
				.profileImageUrl(DEFAULT_PROFILE_IMAGE_URL)
				.build();
	}

	public static User createWithProfileImage(String profileImageUrl) {
		return defaultBuilder()
				.profileImageUrl(profileImageUrl)
				.build();
	}

	public static User createWithoutProfileImage() {
		return defaultBuilder()
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
