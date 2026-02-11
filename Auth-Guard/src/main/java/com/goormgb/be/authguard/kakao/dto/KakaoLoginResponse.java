package com.goormgb.be.authguard.kakao.dto;

import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoLoginResponse {

    private String accessToken;
    private String refreshToken;
    private UserInfo user;
    private boolean onboardingRequired;

    @Getter
    @Builder
    public static class UserInfo {
        private Long userId;
        private String email;
        private String nickname;
        private UserStatus status;
    }

    public static KakaoLoginResponse of(
            String accessToken,
            String refreshToken,
            User user
    ) {
        return KakaoLoginResponse.builder()
                .accessToken(accessToken)
                .user(UserInfo.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .status(user.getStatus())
                        .build())
                .onboardingRequired(!user.getOnboardingCompleted())
                .build();
    }
}
