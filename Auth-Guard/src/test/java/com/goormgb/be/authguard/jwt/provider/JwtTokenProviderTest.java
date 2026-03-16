package com.goormgb.be.authguard.jwt.provider;

import static org.assertj.core.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.goormgb.be.authguard.jwt.config.JwtProperties;
import com.goormgb.be.authguard.jwt.enums.TokenType;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

class JwtTokenProviderTest {

	private static final KeyPair KEY_PAIR;
	private static final String PRIVATE_KEY_BASE64;
	private static final String PUBLIC_KEY_BASE64;

	static {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KEY_PAIR = kpg.generateKeyPair();
			PRIVATE_KEY_BASE64 = Base64.getEncoder().encodeToString(KEY_PAIR.getPrivate().getEncoded());
			PUBLIC_KEY_BASE64 = Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded());
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final Long USER_ID = 42L;
	private static final String ROLE = "ROLE_USER";

	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		JwtProperties properties = new JwtProperties();
		properties.setPrivateKey(PRIVATE_KEY_BASE64);
		properties.setPublicKey(PUBLIC_KEY_BASE64);
		properties.setIssuer("test-issuer");

		JwtProperties.AccessToken accessToken = new JwtProperties.AccessToken();
		accessToken.setAudience("test-audience");
		accessToken.setExpirationMinutes(60);
		properties.setAccessToken(accessToken);

		JwtProperties.RefreshToken refreshToken = new JwtProperties.RefreshToken();
		refreshToken.setAudience("test-refresh-audience");
		refreshToken.setExpirationDays(7);
		properties.setRefreshToken(refreshToken);

		jwtTokenProvider = new JwtTokenProvider(properties);
		jwtTokenProvider.init();
	}

	@Nested
	@DisplayName("createAccessToken")
	class CreateAccessToken {

		@Test
		@DisplayName("ACCESS 토큰을 생성하면 서명이 유효하다")
		void createsValidToken() {
			String token = jwtTokenProvider.createAccessToken(USER_ID, ROLE);

			assertThat(token).isNotBlank();
			assertThat(jwtTokenProvider.validateToken(token)).isTrue();
		}

		@Test
		@DisplayName("ACCESS 토큰의 userId를 정확히 담는다")
		void containsCorrectUserId() {
			String token = jwtTokenProvider.createAccessToken(USER_ID, ROLE);

			assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(USER_ID);
		}

		@Test
		@DisplayName("ACCESS 토큰의 authority를 정확히 담는다")
		void containsCorrectAuthority() {
			String token = jwtTokenProvider.createAccessToken(USER_ID, ROLE);

			assertThat(jwtTokenProvider.getAuthorityFromToken(token)).isEqualTo(ROLE);
		}

		@Test
		@DisplayName("ACCESS 토큰의 tokenType이 ACCESS다")
		void tokenTypeIsAccess() {
			String token = jwtTokenProvider.createAccessToken(USER_ID, ROLE);

			assertThat(jwtTokenProvider.getTokenTypeFromToken(token)).isEqualTo(TokenType.ACCESS);
		}

		@Test
		@DisplayName("ACCESS 토큰은 JTI를 포함한다")
		void containsJti() {
			String token = jwtTokenProvider.createAccessToken(USER_ID, ROLE);

			assertThat(jwtTokenProvider.getJtiFromToken(token)).isNotBlank();
		}

		@Test
		@DisplayName("ACCESS 토큰의 만료 시각은 현재보다 미래다")
		void expirationIsInFuture() {
			String token = jwtTokenProvider.createAccessToken(USER_ID, ROLE);

			assertThat(jwtTokenProvider.getExpirationFromToken(token))
				.isAfter(new java.util.Date());
		}
	}

	@Nested
	@DisplayName("createRefreshToken")
	class CreateRefreshToken {

		@Test
		@DisplayName("REFRESH 토큰을 생성하면 서명이 유효하다")
		void createsValidToken() {
			String token = jwtTokenProvider.createRefreshToken(USER_ID);

			assertThat(token).isNotBlank();
			assertThat(jwtTokenProvider.validateToken(token)).isTrue();
		}

		@Test
		@DisplayName("REFRESH 토큰의 tokenType이 REFRESH다")
		void tokenTypeIsRefresh() {
			String token = jwtTokenProvider.createRefreshToken(USER_ID);

			assertThat(jwtTokenProvider.getTokenTypeFromToken(token)).isEqualTo(TokenType.REFRESH);
		}

		@Test
		@DisplayName("REFRESH 토큰은 userId를 포함한다")
		void containsUserId() {
			String token = jwtTokenProvider.createRefreshToken(USER_ID);

			assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(USER_ID);
		}
	}

	@Nested
	@DisplayName("validateToken")
	class ValidateToken {

		@Test
		@DisplayName("만료된 토큰이면 EXPIRED_TOKEN 예외를 던진다")
		void expiredToken_throwsExpiredTokenException() {
			String expiredToken = buildExpiredToken();

			assertThatThrownBy(() -> jwtTokenProvider.validateToken(expiredToken))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.EXPIRED_TOKEN));
		}

		@Test
		@DisplayName("잘못된 서명의 토큰이면 INVALID_TOKEN 예외를 던진다")
		void wrongSignature_throwsInvalidTokenException() {
			String wrongToken = buildWrongSignatureToken();

			assertThatThrownBy(() -> jwtTokenProvider.validateToken(wrongToken))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_TOKEN));
		}

		@Test
		@DisplayName("형식이 잘못된 토큰이면 INVALID_TOKEN 예외를 던진다")
		void malformedToken_throwsInvalidTokenException() {
			assertThatThrownBy(() -> jwtTokenProvider.validateToken("not.a.jwt"))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_TOKEN));
		}
	}

	@Nested
	@DisplayName("parseClaimsAllowExpired")
	class ParseClaimsAllowExpired {

		@Test
		@DisplayName("만료된 토큰도 Claims를 반환한다")
		void expiredToken_returnsClaims() {
			String expiredToken = buildExpiredToken();

			Claims claims = jwtTokenProvider.parseClaimsAllowExpired(expiredToken);

			assertThat(claims).isNotNull();
			assertThat(claims.getSubject()).isEqualTo(String.valueOf(USER_ID));
		}

		@Test
		@DisplayName("유효한 토큰이면 정상적으로 Claims를 반환한다")
		void validToken_returnsClaims() {
			String token = jwtTokenProvider.createAccessToken(USER_ID, ROLE);

			Claims claims = jwtTokenProvider.parseClaimsAllowExpired(token);

			assertThat(claims.getSubject()).isEqualTo(String.valueOf(USER_ID));
		}

		@Test
		@DisplayName("잘못된 서명의 토큰이면 INVALID_TOKEN 예외를 던진다")
		void wrongSignature_throwsInvalidTokenException() {
			String wrongToken = buildWrongSignatureToken();

			assertThatThrownBy(() -> jwtTokenProvider.parseClaimsAllowExpired(wrongToken))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_TOKEN));
		}
	}

	// ── helpers ──────────────────────────────────────────────────────────────

	private String buildExpiredToken() {
		return Jwts.builder()
			.subject(String.valueOf(USER_ID))
			.claim("tokenType", TokenType.ACCESS.getValue())
			.claim("auth", ROLE)
			.issuedAt(new java.util.Date(System.currentTimeMillis() - 7200_000))
			.expiration(new java.util.Date(System.currentTimeMillis() - 3600_000))
			.signWith((RSAPrivateKey)KEY_PAIR.getPrivate(), Jwts.SIG.RS256)
			.compact();
	}

	private String buildWrongSignatureToken() {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair wrongPair = kpg.generateKeyPair();
			return Jwts.builder()
				.subject(String.valueOf(USER_ID))
				.claim("tokenType", TokenType.ACCESS.getValue())
				.claim("auth", ROLE)
				.issuedAt(new java.util.Date())
				.expiration(new java.util.Date(System.currentTimeMillis() + 3600_000))
				.signWith((RSAPrivateKey)wrongPair.getPrivate(), Jwts.SIG.RS256)
				.compact();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
