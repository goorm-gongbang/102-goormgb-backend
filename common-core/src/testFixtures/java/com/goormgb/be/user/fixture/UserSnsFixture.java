package com.goormgb.be.user.fixture;

import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.UserSns;
import com.goormgb.be.user.enums.SocialProvider;

import org.springframework.test.util.ReflectionTestUtils;

public final class UserSnsFixture {

	public static final SocialProvider DEFAULT_PROVIDER = SocialProvider.KAKAO;
	public static final String DEFAULT_PROVIDER_USER_ID = "kakao_12345";

	private UserSnsFixture() {
	}

	public static UserSns createDefault(User user) {
		return createWithProviderUserId(user, DEFAULT_PROVIDER_USER_ID);
	}

	public static UserSns createWithId(Long id, User user) {
		UserSns userSns = createDefault(user);
		ReflectionTestUtils.setField(userSns, "id", id);
		return userSns;
	}

	public static UserSns createWithProviderUserId(User user, String providerUserId) {
		return UserSns.builder()
				.user(user)
				.provider(DEFAULT_PROVIDER)
				.providerUserId(providerUserId)
				.build();
	}
}