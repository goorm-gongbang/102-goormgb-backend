package com.goormgb.be.authguard.kakao.dto;


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
        static class KakaoAccount {
            private String email;
            private Profile profile;
        }

        @Getter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Profile {
            private String nickname;

            // TODO: 카카오 프로필 이미지 URL 필드 추가. 작업자: 시연
            // @JsonProperty("profile_image_url")
            // private String profileImageUrl;
        }

        public String getEmail() {
            return Optional.ofNullable(kakaoAccount).map(KakaoUserResponse.KakaoAccount::getEmail).orElse(null);
        }

        public String getNickname() {
        return Optional.ofNullable(kakaoAccount)         // 1단계: 계정 확인
                .map(KakaoAccount::getProfile)           // 2단계: 프로필 확인
                .map(Profile::getNickname)               // 3단계: 닉네임 확인
                .orElse(null);
    }

    // TODO: getProfileImageUrl() 추가 - getNickname()과 동일한 Optional 체인 패턴, 작업자: 시연
    // public String getProfileImageUrl() {
    //     return Optional.ofNullable(kakaoAccount)
    //             .map(KakaoAccount::getProfile)
    //             .map(Profile::getProfileImageUrl)
    //             .orElse(null);
    // }
}
