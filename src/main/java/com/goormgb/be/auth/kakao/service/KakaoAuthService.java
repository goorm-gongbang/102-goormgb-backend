package com.goormgb.be.auth.kakao.service;

import com.goormgb.be.auth.kakao.client.KakaoOAuthClient;
import com.goormgb.be.auth.kakao.dto.KakaoLoginResponse;
import com.goormgb.be.auth.kakao.dto.KakaoTokenResponse;
import com.goormgb.be.auth.kakao.dto.KakaoUserResponse;
import com.goormgb.be.auth.provider.JwtTokenProvider;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.UserSns;
import com.goormgb.be.user.enums.SocialProvider;
import com.goormgb.be.user.enums.UserStatus;
import com.goormgb.be.user.repository.UserRepository;
import com.goormgb.be.user.repository.UserSnsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class KakaoAuthService {
    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final UserSnsRepository userSnsRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public KakaoLoginResponse kakaoLogin(String authorizationCode){

        // 1. authorizationCode → 카카오 Access Token 요청
        KakaoTokenResponse kakaoAccessToken = kakaoOAuthClient.requestAccessToken(authorizationCode);

        // 2. 카카오 사용자 정보 조회
        KakaoUserResponse userResponse = kakaoOAuthClient.requestUserInfo(kakaoAccessToken.getAccessToken());
        String providerUserId = String.valueOf(userResponse.getId());
        String email = userResponse.getEmail();
        String nickname = userResponse.getNickname();

        // 3. user_sns 기준으로 기존 사용자 조회
        User user = userSnsRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                providerUserId
        ).map(UserSns::getUser)
                .orElseGet(() -> signUp(email, nickname, providerUserId));

        // 4. 상태 체크
        if (user.getStatus() == UserStatus.DEACTIVATE) {
            throw new CustomException(ErrorCode.USER_DEACTIVATED);
        }

        // 5. 로그인 처리
        user.updateLastLoginAt();

        // 6. JWT 발급
        String accessToken =
                jwtTokenProvider.createAccessToken(user.getId(), authorizationCode);

        String refreshToken =
                jwtTokenProvider.createRefreshToken(user.getId());

        // refreshToken 을 redis 에 저장을 여기서 해야하는지?

        return KakaoLoginResponse.of(accessToken, user);

    }

    /**
     * 신규 사용자 회원가입 처리
     */
    private User signUp(
            String email,
            String nickname,
            String providerUserId
    ) {
        User user = User.createOAuthUser(email, nickname);
        userRepository.save(user);

        UserSns userSns = UserSns.create(
                user,
                SocialProvider.KAKAO,
                providerUserId
        );
        userSnsRepository.save(userSns);

        return user;
    }
}
