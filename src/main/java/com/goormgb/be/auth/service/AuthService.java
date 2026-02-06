package com.goormgb.be.auth.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.auth.dto.RefreshTokenInfo;
import com.goormgb.be.auth.dto.TokenRefreshResponse;
import com.goormgb.be.auth.enums.TokenType;
import com.goormgb.be.auth.provider.JwtTokenProvider;
import com.goormgb.be.auth.repository.RefreshTokenRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
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
    private static final int REFRESH_TOKEN_EXPIRATION_DAYS = 7;

    private final JwtTokenProvider jwtTokenProvider;
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
        if (tokenType != TokenType.REFRESH) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_TYPE);
        }

        // 3. jti, userId 추출
        String jti = jwtTokenProvider.getJtiFromToken(refreshToken);
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 4. Redis에서 저장된 토큰 조회 & 일치 확인
        RefreshTokenInfo storedTokenInfo = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!refreshToken.equals(storedTokenInfo.getToken())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        // 5. 사용자 상태 확인
        User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);
        if (user.getStatus() == UserStatus.DEACTIVATE) {
            throw new CustomException(ErrorCode.USER_DEACTIVATED);
        }

        // 6. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, DEFAULT_AUTHORITY);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        String newJti = jwtTokenProvider.getJtiFromToken(newRefreshToken);

        // 7. Redis 갱신 (기존 토큰 삭제 + 새 토큰 저장)
        refreshTokenRepository.deleteByJti(jti);

        RefreshTokenInfo newTokenInfo = RefreshTokenInfo.builder()
                .userId(userId)
                .token(newRefreshToken)
                .jti(newJti)
                .tokenFamily(storedTokenInfo.getTokenFamily()) // 기존 토큰 패밀리 유지
                .issuedAt(LocalDateTime.now(ZoneOffset.UTC))
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(REFRESH_TOKEN_EXPIRATION_DAYS))
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(getClientIp(request))
                .build();

        refreshTokenRepository.save(newTokenInfo);

        log.debug("Token refreshed - userId: {}, oldJti: {}, newJti: {}", userId, jti, newJti);

        return new TokenRefreshResult(newAccessToken, newRefreshToken);
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
