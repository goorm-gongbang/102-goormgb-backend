package com.goormgb.be.authguard.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.authguard.jwt.config.JwtProperties;
import com.goormgb.be.authguard.auth.dto.RefreshTokenInfo;
import com.goormgb.be.authguard.jwt.provider.JwtTokenProvider;
import com.goormgb.be.authguard.jwt.repository.RefreshTokenRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.user.entity.DevUser;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.repository.DevUserRepository;
import com.goormgb.be.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DevAuthService {

	private static final String DEFAULT_AUTHORITY = "ROLE_USER";

	private final UserRepository userRepository;
	private final DevUserRepository devUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	public void signup(String loginId, String password, String nickname, String email) {
		if (devUserRepository.existsByLoginId(loginId)) {
			throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
		}

		User user = User.builder()
				.email(email)
				.nickname(nickname != null ? nickname : loginId)
				.build();
		userRepository.save(user);

		DevUser devUser = DevUser.builder()
				.loginId(loginId)
				.passwordHash(passwordEncoder.encode(password))
				.user(user)
				.build();
		devUserRepository.save(devUser);

		log.info("Dev user created - loginId: {}, userId: {}", loginId, user.getId());
	}

	@Transactional
	public DevLoginResult login(String loginId, String password, HttpServletRequest request) {
		DevUser devUser = devUserRepository.findByLoginId(loginId)
				.orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(password, devUser.getPasswordHash())) {
			throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
		}

		User user = devUser.getUser();
		user.updateLastLoginAt();

		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), DEFAULT_AUTHORITY);
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
		String jti = jwtTokenProvider.getJtiFromToken(refreshToken);

		Instant now = Instant.now();
		int expirationDays = jwtProperties.getRefreshToken().getExpirationDays();

		RefreshTokenInfo tokenInfo = RefreshTokenInfo.builder()
				.userId(user.getId())
				.token(refreshToken)
				.jti(jti)
				.tokenFamily(UUID.randomUUID().toString())
				.issuedAt(now)
				.expiresAt(now.plus(Duration.ofDays(expirationDays)))
				.userAgent(request.getHeader("User-Agent"))
				.ipAddress(getClientIp(request))
				.build();

		refreshTokenRepository.save(tokenInfo);

		log.info("Dev user logged in - loginId: {}, userId: {}", loginId, user.getId());

		return new DevLoginResult(accessToken, refreshToken);
	}

	private String getClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	public record DevLoginResult(String accessToken, String refreshToken) {
	}
}
