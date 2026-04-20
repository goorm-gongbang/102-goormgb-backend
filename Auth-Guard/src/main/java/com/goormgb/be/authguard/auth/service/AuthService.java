package com.goormgb.be.authguard.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.goormgb.be.authguard.auth.dto.RefreshTokenInfo;
import com.goormgb.be.authguard.auth.dto.UserStatusChangeResponse;
import com.goormgb.be.authguard.auth.dto.WithdrawalResponse;
import com.goormgb.be.authguard.jwt.config.JwtProperties;
import com.goormgb.be.authguard.jwt.enums.TokenType;
import com.goormgb.be.authguard.jwt.provider.JwtTokenProvider;
import com.goormgb.be.authguard.jwt.repository.AccessTokenBlacklistRepository;
import com.goormgb.be.authguard.jwt.repository.RefreshTokenRepository;
import com.goormgb.be.authguard.metrics.AuthMetricsService;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.kafka.EventTopic;
import com.goormgb.be.kafka.event.UserBlockedEvent;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.WithdrawalRequest;
import com.goormgb.be.user.enums.UserStatus;
import com.goormgb.be.user.repository.UserRepository;
import com.goormgb.be.user.repository.WithdrawalRequestRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String DEFAULT_AUTHORITY = "ROLE_USER";

	private final JwtProperties jwtProperties;
	private final JwtTokenProvider jwtTokenProvider;
	private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final WithdrawalRequestRepository withdrawalRequestRepository;
	private final AuthMetricsService authMetricsService;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final UserCacheService userCacheService;
	private final AuthMeService authMeService;

	/**
	 * Refresh Token으로 새로운 Access Token과 Refresh Token을 발급한다. (RTR)
	 *
	 * @param refreshToken 기존 Refresh Token
	 * @param request      HttpServletRequest (IP, User-Agent 추출용)
	 * @return 새 Access Token과 Refresh Token
	 */
	@Transactional
	public TokenRefreshResult refresh(String refreshToken, HttpServletRequest request) {
		// 1. Refresh Token 검증
		jwtTokenProvider.validateToken(refreshToken);

		// 2. Claims 한 번만 파싱하여 필요한 정보 추출
		Claims claims = jwtTokenProvider.parseClaims(refreshToken);
		TokenType tokenType = jwtTokenProvider.getTokenType(claims);
		Preconditions.validate(tokenType == TokenType.REFRESH, ErrorCode.INVALID_TOKEN_TYPE);

		String jti = jwtTokenProvider.getJti(claims);
		Long userId = jwtTokenProvider.getUserId(claims);

		// 3. Redis에서 저장된 토큰 조회 & 일치 확인
		RefreshTokenInfo storedTokenInfo = refreshTokenRepository.findByJtiOrThrow(jti,
				ErrorCode.REFRESH_TOKEN_NOT_FOUND);

		Preconditions.validate(refreshToken.equals(storedTokenInfo.getToken()), ErrorCode.REFRESH_TOKEN_MISMATCH);

		// 4. 사용자 상태 확인
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);
		Preconditions.validate(user.getStatus() != UserStatus.DEACTIVATE, ErrorCode.USER_DEACTIVATED);

		// 5. 기존 sid 추출 (하위 호환: sid 없는 기존 토큰은 새로 생성)
		String sid = Optional.ofNullable(jwtTokenProvider.getSid(claims))
				.or(() -> Optional.ofNullable(storedTokenInfo.getSid()))
				.orElseGet(() -> UUID.randomUUID().toString());

		// 7. 새 토큰 발급 (동일 sid 유지)
		String newAccessToken = jwtTokenProvider.createAccessToken(userId, DEFAULT_AUTHORITY, sid);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, sid);
		String newJti = jwtTokenProvider.getJtiFromToken(newRefreshToken);

		// 8. Redis 갱신 (기존 토큰 삭제 + 새 토큰 저장)
		refreshTokenRepository.deleteByJti(jti);

		Instant now = Instant.now();
		int expirationHours = jwtProperties.getRefreshToken().getExpirationHours();

		RefreshTokenInfo newTokenInfo = RefreshTokenInfo.builder()
				.userId(userId)
				.token(newRefreshToken)
				.jti(newJti)
				.sid(sid)
				.issuedAt(now)
				.expiresAt(now.plus(Duration.ofHours(expirationHours)))
				.userAgent(request.getHeader("User-Agent"))
				.ipAddress(getClientIp(request))
				.build();

		refreshTokenRepository.save(newTokenInfo);

		log.debug("Token refreshed - userId: {}, oldJti: {}, newJti: {}", userId, jti, newJti);

		return new TokenRefreshResult(newAccessToken, newRefreshToken);
	}

	/**
	 * 로그아웃 처리 - Access Token 블랙리스트 등록 및 Redis에서 Refresh Token 삭제
	 *
	 * @param request      HttpServletRequest (Authorization 헤더에서 Access Token 추출)
	 * @param refreshToken 삭제할 Refresh Token
	 */
	public void logout(HttpServletRequest request, String refreshToken) {
		// 1. Access Token 추출 및 블랙리스트 등록
		String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
		Preconditions.validate(
				StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer "),
				ErrorCode.INVALID_TOKEN
		);
		String accessToken = bearerToken.substring(7);

		Claims accessClaims = jwtTokenProvider.parseClaimsAllowExpired(accessToken);
		String accessJti = accessClaims.getId();
		Date expiration = accessClaims.getExpiration();

		long remainingMillis = expiration.getTime() - System.currentTimeMillis();
		if (remainingMillis > 0) {
			accessTokenBlacklistRepository.save(accessJti, Duration.ofMillis(remainingMillis));
		}

		// 2. Refresh Token 파싱 (만료된 토큰도 허용)
		Claims refreshClaims = jwtTokenProvider.parseClaimsAllowExpired(refreshToken);

		// 3. 토큰 타입 확인
		String tokenTypeValue = refreshClaims.get("tokenType", String.class);
		Preconditions.validate(TokenType.REFRESH.getValue().equals(tokenTypeValue), ErrorCode.INVALID_TOKEN_TYPE);

		// 4. jti 추출 후 Redis에서 삭제
		String refreshJti = refreshClaims.getId();
		refreshTokenRepository.deleteByJti(refreshJti);

		log.debug("Logout - accessJti: {}, refreshJti: {}", accessJti, refreshJti);
	}

	private String getClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	/**
	 * 토큰 재발급 결과
	 */
	public record TokenRefreshResult(String accessToken, String refreshToken) {
	}

	@Transactional
	public UserStatusChangeResponse blockUser(Long targetUserId) {
		User user = userRepository.findByIdOrThrow(targetUserId, ErrorCode.USER_NOT_FOUND);

		Preconditions.validate(user.getStatus() != UserStatus.DEACTIVATE, ErrorCode.USER_DEACTIVATED);
		Preconditions.validate(user.getStatus() != UserStatus.BLOCKED, ErrorCode.USER_ALREADY_BLOCKED);

		UserStatus beforeStatus = user.getStatus();
		user.block();
		// 사용자 차단 건수 집계
		authMetricsService.increaseUserBlocked();
		log.info("[User Block] userId={}, status={} -> {}", targetUserId, beforeStatus, user.getStatus());

		// 캐시 무효화 + 차단 이벤트 발행은 트랜잭션 커밋 성공 후 수행 (pre-commit race 방지)
		registerAfterCommitInvalidation(targetUserId, true);

		return UserStatusChangeResponse.from(user);
	}

	@Transactional
	public UserStatusChangeResponse unblockUser(Long targetUserId) {
		User user = userRepository.findByIdOrThrow(targetUserId, ErrorCode.USER_NOT_FOUND);

		Preconditions.validate(user.getStatus() != UserStatus.DEACTIVATE, ErrorCode.USER_DEACTIVATED);
		Preconditions.validate(user.getStatus() != UserStatus.ACTIVATE, ErrorCode.USER_ALREADY_ACTIVE);

		UserStatus beforeStatus = user.getStatus();
		user.unblock();
		// 사용자 차단 해제 건수 집계
		authMetricsService.increaseUserUnblocked();
		log.info("[User Unblock] userId={}, status={} -> {}", targetUserId, beforeStatus, user.getStatus());

		// 캐시 무효화는 트랜잭션 커밋 성공 후에만 수행 (pre-commit race 방지)
		registerAfterCommitInvalidation(targetUserId, false);

		return UserStatusChangeResponse.from(user);
	}

	/**
	 * 회원 탈퇴
	 */
	@Transactional
	public WithdrawalResponse withdraw(Long userId) {
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);

		// 1. 이미 탈퇴 처리된 유저인지 확인
		Preconditions.validate(user.getStatus() != UserStatus.DEACTIVATE, ErrorCode.USER_DEACTIVATED);

		// 2. 결제 완료 유효 티켓 보유 여부 확인
		Preconditions.validate(
				!userRepository.existsValidPaidTicketForWithdrawal(userId),
				ErrorCode.USER_WITHDRAWAL_BLOCKED_BY_VALID_TICKET
		);

		// 3. 환불 처리 진행 중 여부 확인
		Preconditions.validate(
				!userRepository.existsRefundProcessingOrder(userId),
				ErrorCode.USER_WITHDRAWAL_BLOCKED_BY_REFUND_PROCESSING
		);

		// 4. 진행 중인 거래/정산 여부 확인
		Preconditions.validate(
				!userRepository.existsOngoingTransactionOrSettlement(userId),
				ErrorCode.USER_WITHDRAWAL_BLOCKED_BY_ONGOING_TRANSACTION
		);

		// 5. User 상태 변경 (Soft Delete)
		user.deactivate();

		// 6. 탈퇴 요청 데이터 생성 및 저장
		WithdrawalRequest withdrawalRequest = WithdrawalRequest.builder()
				.user(user)
				.build();
		withdrawalRequestRepository.save(withdrawalRequest);

		// 5. 캐시 무효화: 탈퇴 직후 다른 Pod 에서 여전히 ACTIVATE 로 보이지 않도록, 트랜잭션 커밋 후 제거
		registerAfterCommitInvalidation(userId, false);

		return WithdrawalResponse.from(withdrawalRequest);

	}

	/**
	 * 사용자 상태 변경(차단/해제/탈퇴) 직후 트랜잭션이 커밋되고 나서 수행해야 할 후속 작업을 한 번에 등록한다.
	 *
	 * <p>쓰기 트랜잭션 중간에 {@code @CacheEvict} 가 실행되면, 같은 userId 로 들어온 다른 Pod 의
	 * {@code /me} 조회가 아직 커밋 안 된 pre-commit 스냅샷을 읽어 Redis 캐시를 재채움하는 race 가
	 * 가능하다. 이를 막기 위해 evict 와 Kafka 이벤트 발행을 모두 {@code afterCommit} 에 위임한다.</p>
	 *
	 * @param userId 대상 사용자
	 * @param publishBlockedEvent {@code true} 이면 차단 이벤트({@code USER_BLOCKED}) 도 같이 발행
	 */
	private void registerAfterCommitInvalidation(Long userId, boolean publishBlockedEvent) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				userCacheService.evict(userId);
				authMeService.evict(userId);
				if (publishBlockedEvent) {
					publishUserBlockedEvent(userId);
				}
			}
		});
	}

	private void publishUserBlockedEvent(Long userId) {
		UserBlockedEvent event = UserBlockedEvent.builder()
				.userId(userId)
				.occurredAt(Instant.now())
				.build();

		kafkaTemplate.send(EventTopic.USER_BLOCKED, String.valueOf(userId), event)
				.whenComplete((result, ex) -> {
					if (ex != null) {
						log.error("[Kafka] 유저 차단 이벤트 발행 실패 - userId={}", userId, ex);
					} else {
						log.warn("[Kafka] 유저 차단 이벤트 발행 성공 - userId={}", userId);
					}
				});
	}
}
