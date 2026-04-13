package com.goormgb.be.user.dto.response;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.enums.UserStatus;
import com.goormgb.be.user.fixture.UserFixture;

@DisplayName("UserInfoGetResponse DTO 단위 테스트")
class UserInfoGetResponseTest {

	@Test
	@DisplayName("온보딩 미완료 유저 → onboardingRequired = true")
	void from_온보딩_미완료_유저() {
		// given
		User user = UserFixture.createWithId(1L);

		// when
		UserInfoGetResponse response = UserInfoGetResponse.from(user);

		// then
		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(UserStatus.ACTIVATE);
		assertThat(response.email()).isEqualTo(UserFixture.DEFAULT_EMAIL);
		assertThat(response.nickname()).isEqualTo(UserFixture.DEFAULT_NICKNAME);
		assertThat(response.onboardingRequired()).isTrue();
	}

	@Test
	@DisplayName("온보딩 완료 유저 → onboardingRequired = false")
	void from_온보딩_완료_유저() {
		// given
		User user = UserFixture.createOnboardingCompleted();

		// when
		UserInfoGetResponse response = UserInfoGetResponse.from(user);

		// then
		assertThat(response.onboardingRequired()).isFalse();
	}
}
