package com.goormgb.be.authguard.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.authguard.auth.dto.RefreshTokenInfo;
import com.goormgb.be.authguard.jwt.config.JwtProperties;
import com.goormgb.be.authguard.jwt.provider.JwtTokenProvider;
import com.goormgb.be.authguard.jwt.repository.RefreshTokenRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.user.entity.LoadTestUser;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.enums.UserStatus;
import com.goormgb.be.user.repository.LoadTestUserRepository;
import com.goormgb.be.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadTestAuthService {

	private static final String DEFAULT_AUTHORITY = "ROLE_USER";

	private final UserRepository userRepository;
	private final LoadTestUserRepository loadTestUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	public void signup(String loginId, String password) {
		if (loadTestUserRepository.existsByLoginId(loginId)) {
			log.warn("[LoadTest] 부하테스트 중복 아이디 요청 - loginId: {} (이미 존재하는 아이디)", loginId);
			throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
		}

		try {
			User user = User.builder()
					.email(loginId + "@loadtest.com")
					.nickname(loginId)
					.build();
			userRepository.save(user);

			LoadTestUser loadTestUser = LoadTestUser.builder()
					.loginId(loginId)
					.passwordHash(passwordEncoder.encode(password))
					.user(user)
					.build();
			loadTestUserRepository.save(loadTestUser);

			log.info("[LoadTest] 부하테스트 유저 생성 - loginId: {}, userId: {}", loginId, user.getId());
		} catch (DataIntegrityViolationException e) {
            log.warn("[LoadTest] 부하테스트 동시 요청으로 인한 중복 충돌 - loginId: {} (Race Condition 발생)", loginId, e);
			throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
		}
	}

	@Transactional
	public LoadTestLoginResult login(String loginId, String password, HttpServletRequest request) {
		LoadTestUser loadTestUser = loadTestUserRepository.findByLoginId(loginId)
				.orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(password, loadTestUser.getPasswordHash())) {
			throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
		}

		User user = loadTestUser.getUser();

		Preconditions.validate(user.getStatus() != UserStatus.DEACTIVATE, ErrorCode.USER_DEACTIVATED);
		Preconditions.validate(user.getStatus() != UserStatus.BLOCKED, ErrorCode.USER_ALREADY_BLOCKED);

		// 부하테스트 로그인에서는 lastLoginAt 업데이트 생략 (동시 UPDATE 병목 방지)

		String sid = UUID.randomUUID().toString();
		int loadTestMinutes = jwtProperties.getLoadTest().getTokenExpirationMinutes();

		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), DEFAULT_AUTHORITY, sid, loadTestMinutes);
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), sid, loadTestMinutes);
		String jti = jwtTokenProvider.getJtiFromToken(refreshToken);

		Instant now = Instant.now();
		Duration loadTestTtl = Duration.ofMinutes(loadTestMinutes);

		RefreshTokenInfo tokenInfo = RefreshTokenInfo.builder()
				.userId(user.getId())
				.token(refreshToken)
				.jti(jti)
				.sid(sid)
				.issuedAt(now)
				.expiresAt(now.plus(loadTestTtl))
				.userAgent(request.getHeader("User-Agent"))
				.ipAddress(getClientIp(request))
				.build();

		refreshTokenRepository.save(tokenInfo, loadTestTtl);

		return new LoadTestLoginResult(accessToken, refreshToken);
	}

	private String getClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	public record LoadTestLoginResult(String accessToken, String refreshToken) {
	}
}
