package com.goormgb.be.apigateway.fixture;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import com.goormgb.be.apigateway.jwt.enums.TokenType;

import io.jsonwebtoken.Jwts;

public final class JwtTokenFixture {

	/** 테스트용 RSA 키 페어 (서명 전용) */
	private static final KeyPair KEY_PAIR;
	/** 서명 검증 실패 시나리오용 다른 RSA 키 페어 */
	private static final KeyPair WRONG_KEY_PAIR;

	public static final RSAPrivateKey PRIVATE_KEY;
	public static final RSAPublicKey PUBLIC_KEY;
	/** JwtProperties.setPublicKey()에 전달할 base64 DER 문자열 */
	public static final String PUBLIC_KEY_BASE64;

	public static final Long DEFAULT_USER_ID = 1L;
	public static final String DEFAULT_ROLE = "ROLE_USER";
	public static final String DEFAULT_JTI = "test-jti-123";

	static {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KEY_PAIR = kpg.generateKeyPair();
			WRONG_KEY_PAIR = kpg.generateKeyPair();
			PRIVATE_KEY = (RSAPrivateKey)KEY_PAIR.getPrivate();
			PUBLIC_KEY = (RSAPublicKey)KEY_PAIR.getPublic();
			PUBLIC_KEY_BASE64 = Base64.getEncoder().encodeToString(PUBLIC_KEY.getEncoded());
		} catch (NoSuchAlgorithmException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private JwtTokenFixture() {
	}

	public static String createAccessToken(Long userId, String role, String jti) {
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.id(jti)
				.claim("tokenType", TokenType.ACCESS.getValue())
				.claim("auth", role)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 3600_000))
				.signWith(PRIVATE_KEY, Jwts.SIG.RS256)
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
				.signWith(PRIVATE_KEY, Jwts.SIG.RS256)
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
				.signWith(PRIVATE_KEY, Jwts.SIG.RS256)
				.compact();
	}

	/** 다른 RSA 키쌍으로 서명 → 검증 실패 시나리오 */
	public static String createWrongSignatureToken() {
		return Jwts.builder()
				.subject(String.valueOf(DEFAULT_USER_ID))
				.claim("tokenType", TokenType.ACCESS.getValue())
				.claim("auth", DEFAULT_ROLE)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 3600_000))
				.signWith((RSAPrivateKey)WRONG_KEY_PAIR.getPrivate(), Jwts.SIG.RS256)
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
				.signWith(PRIVATE_KEY, Jwts.SIG.RS256)
				.compact();
	}
}
