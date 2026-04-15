package com.goormgb.be.authguard.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.goormgb.be.authguard.auth.dto.WithdrawalResponse;
import com.goormgb.be.authguard.jwt.config.JwtProperties;
import com.goormgb.be.authguard.jwt.provider.JwtTokenProvider;
import com.goormgb.be.authguard.jwt.repository.AccessTokenBlacklistRepository;
import com.goormgb.be.authguard.jwt.repository.RefreshTokenRepository;
import com.goormgb.be.authguard.metrics.AuthMetricsService;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.WithdrawalRequest;
import com.goormgb.be.user.enums.UserStatus;
import com.goormgb.be.user.fixture.UserFixture;
import com.goormgb.be.user.repository.UserRepository;
import com.goormgb.be.user.repository.WithdrawalRequestRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 서비스 단위 테스트")
class AuthServiceTest {

	@Mock
	private JwtProperties jwtProperties;
	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private AccessTokenBlacklistRepository accessTokenBlacklistRepository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private WithdrawalRequestRepository withdrawalRequestRepository;
	@Mock
	private AuthMetricsService authMetricsService;
	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	@InjectMocks
	private AuthService authService;

	@Nested
	@DisplayName("withdraw — 회원 탈퇴")
	class Withdraw {

		@Test
		@DisplayName("유효 티켓과 환불 진행 주문이 없으면 탈퇴에 성공한다")
		void withdraw_성공() {
			Long userId = 1L;
			User user = UserFixture.createWithId(userId);

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userRepository.existsValidPaidTicketForWithdrawal(userId)).willReturn(false);
			given(userRepository.existsRefundProcessingOrder(userId)).willReturn(false);
			given(userRepository.existsOngoingTransactionOrSettlement(userId)).willReturn(false);
			given(withdrawalRequestRepository.save(any(WithdrawalRequest.class)))
					.willAnswer(invocation -> invocation.getArgument(0));

			WithdrawalResponse response = authService.withdraw(userId);

			assertThat(user.getStatus()).isEqualTo(UserStatus.DEACTIVATE);
			assertThat(response.status()).isEqualTo(UserStatus.DEACTIVATE.name());
			assertThat(response.withdrawnAt()).isNotNull();
			assertThat(response.reactivateUntil()).isNotNull();
			then(withdrawalRequestRepository).should().save(any(WithdrawalRequest.class));
		}

		@Test
		@DisplayName("결제 완료 유효 티켓이 있으면 탈퇴를 제한한다")
		void withdraw_유효티켓_보유시_제한() {
			Long userId = 1L;
			User user = UserFixture.createWithId(userId);

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userRepository.existsValidPaidTicketForWithdrawal(userId)).willReturn(true);

			assertThatThrownBy(() -> authService.withdraw(userId))
					.isInstanceOfSatisfying(CustomException.class,
						ex -> assertThat(ex.getErrorCode())
							.isEqualTo(ErrorCode.USER_WITHDRAWAL_BLOCKED_BY_VALID_TICKET));

			then(withdrawalRequestRepository).should(never()).save(any(WithdrawalRequest.class));
			then(userRepository).should(never()).existsRefundProcessingOrder(anyLong());
			then(userRepository).should(never()).existsOngoingTransactionOrSettlement(anyLong());
		}

		@Test
		@DisplayName("환불 처리 진행 중인 주문이 있으면 탈퇴를 제한한다")
		void withdraw_환불처리중_주문존재시_제한() {
			Long userId = 1L;
			User user = UserFixture.createWithId(userId);

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userRepository.existsValidPaidTicketForWithdrawal(userId)).willReturn(false);
			given(userRepository.existsRefundProcessingOrder(userId)).willReturn(true);

			assertThatThrownBy(() -> authService.withdraw(userId))
					.isInstanceOfSatisfying(CustomException.class,
						ex -> assertThat(ex.getErrorCode())
							.isEqualTo(ErrorCode.USER_WITHDRAWAL_BLOCKED_BY_REFUND_PROCESSING));

			then(withdrawalRequestRepository).should(never()).save(any(WithdrawalRequest.class));
			then(userRepository).should(never()).existsOngoingTransactionOrSettlement(anyLong());
		}

		@Test
		@DisplayName("진행 중인 거래/정산 건이 있으면 탈퇴를 제한한다")
		void withdraw_진행중거래정산_존재시_제한() {
			Long userId = 1L;
			User user = UserFixture.createWithId(userId);

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userRepository.existsValidPaidTicketForWithdrawal(userId)).willReturn(false);
			given(userRepository.existsRefundProcessingOrder(userId)).willReturn(false);
			given(userRepository.existsOngoingTransactionOrSettlement(userId)).willReturn(true);

			assertThatThrownBy(() -> authService.withdraw(userId))
					.isInstanceOfSatisfying(CustomException.class,
						ex -> assertThat(ex.getErrorCode())
							.isEqualTo(ErrorCode.USER_WITHDRAWAL_BLOCKED_BY_ONGOING_TRANSACTION));

			then(withdrawalRequestRepository).should(never()).save(any(WithdrawalRequest.class));
		}
	}
}
