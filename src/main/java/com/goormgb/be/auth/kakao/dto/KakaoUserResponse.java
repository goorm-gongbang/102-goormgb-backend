package com.goormgb.be.auth.kakao.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
        return kakaoAccount != null ? kakaoAccount.email : null;
    }

    public String getNickname() {
        return (kakaoAccount != null && kakaoAccount.profile != null)
                ? kakaoAccount.profile.nickname
                : null;
    }
}
