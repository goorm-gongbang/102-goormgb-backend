package com.goormgb.be.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.goormgb.be.auth.config.JwtProperties;

import io.jsonwebtoken.Claims;

import com.goormgb.be.auth.dto.RefreshTokenInfo;
import com.goormgb.be.auth.enums.TokenType;
import com.goormgb.be.auth.provider.JwtTokenProvider;
import com.goormgb.be.auth.repository.AccessTokenBlacklistRepository;
import com.goormgb.be.auth.repository.RefreshTokenRepository;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.enums.UserStatus;
import com.goormgb.be.user.repository.UserRepository;

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

		// 2. 토큰 타입 확인
		TokenType tokenType = jwtTokenProvider.getTokenTypeFromToken(refreshToken);
		Preconditions.validate(tokenType == TokenType.REFRESH, ErrorCode.INVALID_TOKEN_TYPE);

		// 3. jti, userId 추출
		String jti = jwtTokenProvider.getJtiFromToken(refreshToken);
		Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

		// 4. Redis에서 저장된 토큰 조회 & 일치 확인
		RefreshTokenInfo storedTokenInfo = refreshTokenRepository.findByJtiOrThrow(jti,
				ErrorCode.REFRESH_TOKEN_NOT_FOUND);

		Preconditions.validate(refreshToken.equals(storedTokenInfo.getToken()), ErrorCode.REFRESH_TOKEN_MISMATCH);

		// 5. 사용자 상태 확인
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);
		Preconditions.validate(user.getStatus() != UserStatus.DEACTIVATE, ErrorCode.USER_DEACTIVATED);

		// 6. 새 토큰 발급
		String newAccessToken = jwtTokenProvider.createAccessToken(userId, DEFAULT_AUTHORITY);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
		String newJti = jwtTokenProvider.getJtiFromToken(newRefreshToken);

		// 7. Redis 갱신 (기존 토큰 삭제 + 새 토큰 저장)
		refreshTokenRepository.deleteByJti(jti);

		// LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		Instant now = Instant.now();
		int expirationDays = jwtProperties.getRefreshToken().getExpirationDays();

		RefreshTokenInfo newTokenInfo = RefreshTokenInfo.builder()
				.userId(userId)
				.token(newRefreshToken)
				.jti(newJti)
				.tokenFamily(storedTokenInfo.getTokenFamily()) // 기존 토큰 패밀리 유지
				.issuedAt(now)
				// .expiresAt(now.plusDays(expirationDays))
				.expiresAt(now.plus(Duration.ofDays(expirationDays)))
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
}
