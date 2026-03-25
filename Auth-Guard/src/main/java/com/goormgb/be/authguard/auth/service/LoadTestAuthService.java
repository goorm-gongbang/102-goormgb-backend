package com.goormgb.be.authguard.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.authguard.auth.dto.RefreshTokenInfo;
import com.goormgb.be.authguard.jwt.config.JwtProperties;
import com.goormgb.be.authguard.jwt.provider.JwtTokenProvider;
import com.goormgb.be.authguard.jwt.repository.RefreshTokenRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.user.entity.LoadTestUser;
import com.goormgb.be.user.entity.User;
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
			throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
		}

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
	}

	@Transactional
	public LoadTestLoginResult login(String loginId, String password, HttpServletRequest request) {
		LoadTestUser loadTestUser = loadTestUserRepository.findByLoginId(loginId)
				.orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(password, loadTestUser.getPasswordHash())) {
			throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
		}

		User user = loadTestUser.getUser();
		user.updateLastLoginAt();

		String sid = UUID.randomUUID().toString();
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), DEFAULT_AUTHORITY, sid);
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), sid);
		String jti = jwtTokenProvider.getJtiFromToken(refreshToken);

		Instant now = Instant.now();
		int expirationDays = jwtProperties.getRefreshToken().getExpirationDays();

		RefreshTokenInfo tokenInfo = RefreshTokenInfo.builder()
				.userId(user.getId())
				.token(refreshToken)
				.jti(jti)
				.sid(sid)
				.issuedAt(now)
				.expiresAt(now.plus(Duration.ofDays(expirationDays)))
				.userAgent(request.getHeader("User-Agent"))
				.ipAddress(getClientIp(request))
				.build();

		refreshTokenRepository.save(tokenInfo);

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
