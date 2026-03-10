package com.goormgb.be.authguard.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	/** PKCS#8 형식 RSA 개인키 (base64 DER). Auth-Guard 전용 — 서명에 사용. */
	private String privateKey;
	/** X.509 형식 RSA 공개키 (base64 DER). 모든 서비스 공유 — 검증에 사용. */
	private String publicKey;
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
