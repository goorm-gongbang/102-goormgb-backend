package com.goormgb.be.auth.kakao.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "kakao")
public class KakaoOAuthProperties {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    private final String authUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
}
