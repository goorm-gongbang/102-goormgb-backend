package com.goormgb.be.auth.kakao.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * 카카오 사용자 정보 응답 DTO
 *
 * id                 → provider_user_id
 * profile_nickname   → nickname
 * account_email      → email
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserResponse {
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;


    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class KakaoAccount {
        private String email;
        private Profile profile;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Profile {
        private String nickname;
    }

    public String getEmail() {
        return Optional.ofNullable(kakaoAccount).map(KakaoAccount::getEmail).orElse(null);
    }

    public String getNickname() {
        return java.util.Optional.ofNullable(kakaoAccount) // 1단계: 계정 확인
                .map(KakaoAccount::getProfile)           // 2단계: 프로필 확인
                .map(Profile::getNickname)               // 3단계: 닉네임 확인
                .orElse(null);
    }
}
