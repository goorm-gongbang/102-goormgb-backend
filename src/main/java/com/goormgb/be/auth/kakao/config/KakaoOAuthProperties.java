package com.goormgb.be.auth.kakao.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@RequiredArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "kakao")
public class KakaoOAuthProperties {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    private final String authUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
}
