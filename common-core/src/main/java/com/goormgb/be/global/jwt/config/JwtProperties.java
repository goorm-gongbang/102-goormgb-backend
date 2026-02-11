package com.goormgb.be.global.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	private String secretKey;
	private String issuer;
	private AccessToken accessToken = new AccessToken();
	private RefreshToken refreshToken = new RefreshToken();
	private Cookie cookie = new Cookie();

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

	@Getter
	@Setter
	public static class Cookie {
		private boolean secure = true;
	}
}