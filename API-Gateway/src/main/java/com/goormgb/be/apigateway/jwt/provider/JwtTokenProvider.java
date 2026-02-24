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

	public void validateToken(String token) {
		try {
			parseClaimsFromToken(token);
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			throw new IllegalArgumentException("Invalid JWT token", e);
		}
	}

	public Long getUserIdFromToken(String token) {
		return Long.parseLong(parseClaimsFromToken(token).getSubject());
	}

	public String getAuthorityFromToken(String token) {
		return parseClaimsFromToken(token).get(CLAIM_AUTH, String.class);
	}

	public TokenType getTokenTypeFromToken(String token) {
		String tokenTypeValue = parseClaimsFromToken(token).get(CLAIM_TOKEN_TYPE, String.class);
		return TokenType.valueOf(tokenTypeValue);
	}

	public String getJtiFromToken(String token) {
		return parseClaimsFromToken(token).getId();
	}

	private Claims parseClaimsFromToken(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
