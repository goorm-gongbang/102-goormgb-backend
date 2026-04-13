package com.goormgb.be.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.user.dto.response.UserInfoGetResponse;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.fixture.UserFixture;
import com.goormgb.be.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	@Test
	@DisplayName("getMyInfo - 온보딩 미완료 유저 조회 시 onboardingRequired = true")
	void getMyInfo_온보딩_미완료() {
		// given
		User user = UserFixture.createWithId(1L);
		given(userRepository.findByIdOrThrow(1L, ErrorCode.USER_NOT_FOUND)).willReturn(user);

		// when
		UserInfoGetResponse response = userService.getMyInfo(1L);

		// then
		assertThat(response.onboardingRequired()).isTrue();
		assertThat(response.email()).isEqualTo(UserFixture.DEFAULT_EMAIL);
	}

	@Test
	@DisplayName("getMyInfo - 온보딩 완료 유저 조회 시 onboardingRequired = false")
	void getMyInfo_온보딩_완료() {
		// given
		User user = UserFixture.createOnboardingCompleted();
		org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 2L);
		given(userRepository.findByIdOrThrow(2L, ErrorCode.USER_NOT_FOUND)).willReturn(user);

		// when
		UserInfoGetResponse response = userService.getMyInfo(2L);

		// then
		assertThat(response.onboardingRequired()).isFalse();
	}
}
