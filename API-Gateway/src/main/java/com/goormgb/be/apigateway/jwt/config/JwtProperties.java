package com.goormgb.be.apigateway.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	/** X.509 형식 RSA 공개키 (base64 DER). 검증 전용 — 서명 불가. */
	private String publicKey;
	private String issuer;
	private AccessToken accessToken = new AccessToken();

	@Getter
	@Setter
	public static class AccessToken {
		private String audience;
	}
}
