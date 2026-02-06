package com.goormgb.be.auth.kakao.client;


import com.goormgb.be.auth.kakao.config.KakaoOAuthProperties;
import com.goormgb.be.auth.kakao.dto.KakaoTokenResponse;
import com.goormgb.be.auth.kakao.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


/**
  * 카카오 OAuth 서버와 통신만 담당
 * 인가 코드 → Access Token
 * Access Token → 사용자 정보
 */
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final KakaoOAuthProperties properties;

    // 외부 API 호출 전용
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 1. 카카오 로그인 페이지 URL 생성
     * 프론트에서 이 URL로 location.href 이동
     */
    public String createLoginUrl() {
        return properties.getAuthUrl()
                + "?response_type=code"
                + "&client_id=" + properties.getClientId()
                + "&redirect_uri=" + properties.getRedirectUri();
    }

    /**
     * 2.인가 코드 → 카카오 Access Token 요청
     *
     * @param authorizationCode 카카오 로그인 성공 후 받은 code
     * @return 카카오 Access Token
     */
    public String requestAccessToken(String authorizationCode){
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("grant_type", "authorization_code");
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("redirect_uri", properties.getRedirectUri());
        params.add("code", authorizationCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        KakaoTokenResponse response = restTemplate.postForObject(
                properties.getTokenUrl(),
                request,
                KakaoTokenResponse.class);

        return response.getAccessToken();
    }

    /**
     * 3.카카오 Access Token으로 사용자 정보 조회
     *
     * id (카카오 고유 ID)
     * profile_nickname
     * account_email
     */
    public KakaoUserResponse requestUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Void> requset = new HttpEntity<>(headers);

        return restTemplate.postForObject(
                properties.getUserInfoUrl(),
                requset,
                KakaoUserResponse.class,
                Map.of()
        );

    }

}
