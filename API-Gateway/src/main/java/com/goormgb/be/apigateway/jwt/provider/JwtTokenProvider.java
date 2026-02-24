package com.goormgb.be.apigateway.jwt.provider;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.goormgb.be.apigateway.jwt.config.JwtProperties;
import com.goormgb.be.apigateway.jwt.enums.TokenType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private static final String CLAIM_TOKEN_TYPE = "tokenType";
	private static final String CLAIM_AUTH = "auth";

	private final JwtProperties jwtProperties;
	private SecretKey secretKey;

	@PostConstruct
	public void init() {
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
	}

	/**
	 * JWT를 한 번만 파싱하여 Claims를 반환한다.
	 * 필터에서 이 메서드를 단 한 번 호출하고 반환된 Claims에서 필요한 정보를 추출해야 한다.
	 */
	public Claims parseClaims(String token) {
		try {
			return Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			throw new IllegalArgumentException("Invalid JWT token", e);
		}
	}

	public TokenType getTokenType(Claims claims) {
		return TokenType.valueOf(claims.get(CLAIM_TOKEN_TYPE, String.class));
	}

	public Long getUserId(Claims claims) {
		return Long.parseLong(claims.getSubject());
	}

	public String getAuthority(Claims claims) {
		return claims.get(CLAIM_AUTH, String.class);
	}

	public String getJti(Claims claims) {
		return claims.getId();
	}
}
