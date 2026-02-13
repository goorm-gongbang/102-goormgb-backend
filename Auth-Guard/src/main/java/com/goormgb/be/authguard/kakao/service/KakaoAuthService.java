package com.goormgb.be.authguard.kakao.service;

import com.goormgb.be.global.jwt.config.JwtProperties;
import com.goormgb.be.authguard.auth.dto.RefreshTokenInfo;
import com.goormgb.be.authguard.kakao.client.KakaoOAuthClient;
import com.goormgb.be.authguard.kakao.dto.KakaoLoginResponse;
import com.goormgb.be.authguard.kakao.dto.KakaoTokenResponse;
import com.goormgb.be.authguard.kakao.dto.KakaoUserResponse;
import com.goormgb.be.global.jwt.provider.JwtTokenProvider;
import com.goormgb.be.authguard.jwt.repository.RefreshTokenRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.UserSns;
import com.goormgb.be.user.enums.SocialProvider;
import com.goormgb.be.user.enums.UserStatus;
import com.goormgb.be.user.repository.UserRepository;
import com.goormgb.be.user.repository.UserSnsRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class KakaoAuthService {
    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final UserSnsRepository userSnsRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    public KakaoLoginResponse kakaoLogin(String authorizationCode, HttpServletRequest request){

        // 1. authorizationCode → 카카오 Access Token 요청
        KakaoTokenResponse kakaoAccessToken = kakaoOAuthClient.requestAccessToken(authorizationCode);

        Optional.ofNullable(kakaoAccessToken)
                .filter(token -> token.getAccessToken() != null)
                .orElseThrow(() -> new CustomException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED));

        // 2. 카카오 사용자 정보 조회
        KakaoUserResponse userResponse = kakaoOAuthClient.requestUserInfo(kakaoAccessToken.getAccessToken());
        String providerUserId = String.valueOf(userResponse.getId());
        String email = userResponse.getEmail();
        String nickname = userResponse.getNickname();
        String profileImageUrl = userResponse.getProfileImageUrl();

        // 3. user_sns 기준으로 기존 사용자 조회
        User user = userSnsRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                providerUserId
        ).map(UserSns::getUser)
                .orElseGet(() -> signUp(email, nickname, profileImageUrl, providerUserId));

        // 4. 상태 체크
        Preconditions.validate(
                user.getStatus() != UserStatus.DEACTIVATE,
                ErrorCode.USER_DEACTIVATED
        );

        // 5. 로그인 처리
        user.updateLastLoginAt();

        // 6. JWT 발급
        String accessToken =
                jwtTokenProvider.createAccessToken(user.getId(), "ROLE_USER");

        String refreshToken =
                jwtTokenProvider.createRefreshToken(user.getId());

        String jti = jwtTokenProvider.getJtiFromToken(refreshToken);

        // 7. refreshToken redis 에 저장
        //LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Instant now = Instant.now();

        int expirationDays = jwtProperties.getRefreshToken().getExpirationDays();

        RefreshTokenInfo tokenInfo = RefreshTokenInfo.builder()
                .userId(user.getId())
                .token(refreshToken)
                .jti(jti)
                .tokenFamily(UUID.randomUUID().toString()) // 신규 로그인이므로 새로운 토큰 패밀리 생성
                .issuedAt(now)
                //.expiresAt(now.plusDays(expirationDays))
                .expiresAt(now.plus(Duration.ofDays(expirationDays)))
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(getClientIp(request))
                .build();

        refreshTokenRepository.save(tokenInfo);

        // 컨트롤러에서 쿠키 설정을 위해 응답 DTO에 refreshToken을 잠시 포함하거나
        // 서비스 결과 객체를 따로 만들어 리턴하는 것이 좋습니다.
        return KakaoLoginResponse.of(accessToken, refreshToken, user);

    }

    /**
     * 신규 사용자 회원가입 처리
     */
    private User signUp(
            String email,
            String nickname,
            String profileImageUrl,
            String providerUserId
    ) {
        User user = User.createOAuthUser(email, nickname, profileImageUrl);
        userRepository.save(user);

        UserSns userSns = UserSns.create(
                user,
                SocialProvider.KAKAO,
                providerUserId
        );
        userSnsRepository.save(userSns);

        return user;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
