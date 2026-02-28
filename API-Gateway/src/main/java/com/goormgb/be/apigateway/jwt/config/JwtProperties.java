package com.goormgb.be.apigateway.jwt.config;

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

	@Getter
	@Setter
	public static class AccessToken {
		private String audience;
	}
}
