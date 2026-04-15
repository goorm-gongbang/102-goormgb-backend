package com.goormgb.be.ordercore.mypage.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.mypage.dto.request.MyPageAccountUpdateRequest;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageAccountResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.order.repository.OrderMyPageSummaryCounts;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.UserSns;
import com.goormgb.be.ordercore.user.service.UserCacheService;
import com.goormgb.be.user.enums.SocialProvider;
import com.goormgb.be.user.repository.UserRepository;
import com.goormgb.be.user.repository.UserSnsRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyPageProfileService 서비스 단위 테스트")
class MyPageProfileServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserSnsRepository userSnsRepository;
	@Mock
	private OrderRepository orderRepository;
	@Mock
	private UserCacheService userCacheService;

	private MyPageProfileService myPageProfileService;
	private Clock clock;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2026-03-26T00:00:00Z"), ZoneOffset.UTC);
		myPageProfileService = new MyPageProfileService(
			userRepository,
			userSnsRepository,
			orderRepository,
			userCacheService,
			clock
		);
	}

	private UserSns createUserSns(User user) {
		return UserSns.builder()
			.user(user)
			.provider(SocialProvider.KAKAO)
			.providerUserId("kakao-12345")
			.build();
	}

	@Nested
	@DisplayName("updateAccount — 개인정보 수정")
	class UpdateAccount {

		@Test
		@DisplayName("유효한 닉네임이면 계정 정보가 수정된다")
		void updateAccount_성공() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			UserSns userSns = createUserSns(user);

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userSnsRepository.findByUserId(userId)).willReturn(Optional.of(userSns));

			MyPageAccountResponse response = myPageProfileService.updateAccount(
				userId,
				new MyPageAccountUpdateRequest("  goorm_new  ")
			);

			assertThat(response.nickname()).isEqualTo("goorm_new");
			assertThat(response.email()).isEqualTo("test@test.com");
			assertThat(response.profileImageUrl()).isNull();
			assertThat(response.snsAccount()).isNotNull();
			assertThat(response.snsAccount().provider()).isEqualTo("KAKAO");
			assertThat(user.getNickname()).isEqualTo("goorm_new");
		}

		@Test
		@DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
		void updateAccount_사용자없음_예외() {
			Long userId = 999L;
			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			assertThatThrownBy(
				() -> myPageProfileService.updateAccount(userId, new MyPageAccountUpdateRequest("goorm_new")))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
		}
	}

	@Nested
	@DisplayName("getAccount — 개인정보 조회")
	class GetAccount {

		@Test
		@DisplayName("유효한 userId이면 계정 정보를 반환한다")
		void getAccount_성공() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			UserSns userSns = createUserSns(user);

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userSnsRepository.findByUserId(userId)).willReturn(Optional.of(userSns));

			MyPageAccountResponse response = myPageProfileService.getAccount(userId);

			assertThat(response.email()).isEqualTo("test@test.com");
			assertThat(response.nickname()).isEqualTo("테스터");
			assertThat(response.profileImageUrl()).isNull();
			assertThat(response.snsAccount()).isNotNull();
			assertThat(response.snsAccount().provider()).isEqualTo("KAKAO");
		}

		@Test
		@DisplayName("SNS 정보가 없으면 snsAccount는 null이다")
		void getAccount_SNS없음_null() {
			Long userId = 1L;
			User user = OrderFixture.createUser();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userSnsRepository.findByUserId(userId)).willReturn(Optional.empty());

			MyPageAccountResponse response = myPageProfileService.getAccount(userId);

			assertThat(response.snsAccount()).isNull();
		}

		@Test
		@DisplayName("존재하지 않는 userId이면 USER_NOT_FOUND 예외가 발생한다")
		void getAccount_사용자없음_예외() {
			Long userId = 999L;

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			assertThatThrownBy(() -> myPageProfileService.getAccount(userId))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
		}
	}

	@Test
	@DisplayName("유효한 userId이면 프로필과 티켓 요약을 반환한다")
	void getProfile_성공() {
		Long userId = 1L;
		User user = OrderFixture.createUser();
		UserSns userSns = createUserSns(user);

		given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
		given(userSnsRepository.findByUserId(userId)).willReturn(Optional.of(userSns));
		given(orderRepository.findMyPageSummaryCounts(eq(userId), any(), any(), any(), any(), any()))
			.willReturn(new OrderMyPageSummaryCounts(8L, 2L, 1L, 1L, 5L));

		MyPageProfileResponse response = myPageProfileService.getProfile(userId);

		assertThat(response).isNotNull();
		assertThat(response.profile().nickname()).isEqualTo("테스터");
		assertThat(response.profile().snsProvider()).isEqualTo("KAKAO");
		assertThat(response.ticketSummary().upcomingCount()).isEqualTo(2);
		assertThat(response.ticketSummary().cancelRefundCount()).isEqualTo(1);
		assertThat(response.ticketSummary().completedCount()).isEqualTo(5);
	}

	@Test
	@DisplayName("SNS 정보가 없으면 snsProvider는 null이다")
	void getProfile_SNS없음_null() {
		Long userId = 1L;
		User user = OrderFixture.createUser();

		given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
		given(userSnsRepository.findByUserId(userId)).willReturn(Optional.empty());
		given(orderRepository.findMyPageSummaryCounts(eq(userId), any(), any(), any(), any(), any()))
			.willReturn(new OrderMyPageSummaryCounts(0L, 0L, 0L, 0L, 0L));

		MyPageProfileResponse response = myPageProfileService.getProfile(userId);

		assertThat(response.profile().snsProvider()).isNull();
	}

	@Test
	@DisplayName("존재하지 않는 userId이면 USER_NOT_FOUND 예외가 발생한다")
	void getProfile_사용자_미발견_예외() {
		Long userId = 999L;

		given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND))
			.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

		assertThatThrownBy(() -> myPageProfileService.getProfile(userId))
			.isInstanceOf(CustomException.class)
			.hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("모든 카운트가 0이어도 정상 반환한다")
	void getProfile_카운트_모두_0() {
		Long userId = 1L;
		User user = OrderFixture.createUser();

		given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
		given(userSnsRepository.findByUserId(userId)).willReturn(Optional.empty());
		given(orderRepository.findMyPageSummaryCounts(eq(userId), any(), any(), any(), any(), any()))
			.willReturn(new OrderMyPageSummaryCounts(0L, 0L, 0L, 0L, 0L));

		MyPageProfileResponse response = myPageProfileService.getProfile(userId);

		assertThat(response.ticketSummary().upcomingCount()).isEqualTo(0);
		assertThat(response.ticketSummary().cancelRefundCount()).isEqualTo(0);
		assertThat(response.ticketSummary().completedCount()).isEqualTo(0);
	}
}
