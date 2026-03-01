package com.goormgb.be.apigateway.fixture;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.goormgb.be.apigateway.jwt.enums.TokenType;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public final class JwtTokenFixture {

	public static final String SECRET_KEY = "goormgb-test-secret-key-do-not-use-in-production-20260321-must-be-at-least-256-bits";
	public static final String WRONG_SECRET_KEY = "wrong-secret-key-that-is-long-enough-for-hmac-sha256!!";
	public static final Long DEFAULT_USER_ID = 1L;
	public static final String DEFAULT_ROLE = "ROLE_USER";
	public static final String DEFAULT_JTI = "test-jti-123";

	private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

	private JwtTokenFixture() {
	}

	public static SecretKey secretKey() {
		return KEY;
	}

	public static String createAccessToken(Long userId, String role, String jti) {
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.id(jti)
				.claim("tokenType", TokenType.ACCESS.getValue())
				.claim("auth", role)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 3600_000))
				.signWith(KEY)
				.compact();
	}

	public static String createDefaultAccessToken() {
		return createAccessToken(DEFAULT_USER_ID, DEFAULT_ROLE, UUID.randomUUID().toString());
	}

	public static String createAccessTokenWithJti(String jti) {
		return createAccessToken(DEFAULT_USER_ID, DEFAULT_ROLE, jti);
	}

	public static String createRefreshToken(Long userId) {
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.id(UUID.randomUUID().toString())
				.claim("tokenType", TokenType.REFRESH.getValue())
				.claim("auth", DEFAULT_ROLE)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 3600_000))
				.signWith(KEY)
				.compact();
	}

	public static String createExpiredAccessToken() {
		return Jwts.builder()
				.subject(String.valueOf(DEFAULT_USER_ID))
				.id(UUID.randomUUID().toString())
				.claim("tokenType", TokenType.ACCESS.getValue())
				.claim("auth", DEFAULT_ROLE)
				.issuedAt(new Date(System.currentTimeMillis() - 7200_000))
				.expiration(new Date(System.currentTimeMillis() - 3600_000))
				.signWith(KEY)
				.compact();
	}

	public static String createWrongSignatureToken() {
		SecretKey wrongKey = Keys.hmacShaKeyFor(WRONG_SECRET_KEY.getBytes());
		return Jwts.builder()
				.subject(String.valueOf(DEFAULT_USER_ID))
				.claim("tokenType", TokenType.ACCESS.getValue())
				.claim("auth", DEFAULT_ROLE)
				.signWith(wrongKey)
				.compact();
	}

	public static String createTokenWithCustomExpiration(Long userId, String role, TokenType tokenType,
			String jti, Date expiration) {
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.id(jti)
				.claim("tokenType", tokenType.getValue())
				.claim("auth", role)
				.issuedAt(new Date())
				.expiration(expiration)
				.signWith(KEY)
				.compact();
	}
}