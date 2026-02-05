package com.goormgb.be.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;
// TODO: JWT Properties 클래스 생성
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey;
    private String issuer;
    private AccessToken accessToken = new AccessToken();
    private RefreshToken refreshToken = new RefreshToken();

    @Getter
    @Setter
    public static class AccessToken {
        private String audience;
        private int expirationMinutes;
    }

    @Getter
    @Setter
    public static class RefreshToken {
        private String audience;
        private int expirationDays;
    }
}
