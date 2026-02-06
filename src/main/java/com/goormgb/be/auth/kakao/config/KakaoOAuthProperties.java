package com.goormgb.be.auth.kakao.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kakao")
public class KakaoOAuthProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    private String authUrl;
    private String tokenUrl;
    private String userInfoUrl;
}
