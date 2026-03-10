package com.goormgb.be.apigateway.jwt.provider;

import static org.assertj.core.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.goormgb.be.apigateway.fixture.JwtTokenFixture;
import com.goormgb.be.apigateway.jwt.config.JwtProperties;
import com.goormgb.be.apigateway.jwt.enums.TokenType;

import io.jsonwebtoken.Claims;

class JwtTokenProviderTest {

	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		JwtProperties properties = new JwtProperties();
		properties.setPublicKey(JwtTokenFixture.PUBLIC_KEY_BASE64);
		properties.setIssuer("test-issuer");

		jwtTokenProvider = new JwtTokenProvider(properties);
		jwtTokenProvider.init();
	}

	@Nested
	@DisplayName("parseClaims")
	class ParseClaims {

		@Test
		@DisplayName("유효한 토큰이면 Claims를 반환한다")
		void validToken_returnsClaims() {
			String token = JwtTokenFixture.createDefaultAccessToken();

			Claims claims = jwtTokenProvider.parseClaims(token);

			assertThat(claims).isNotNull();
			assertThat(claims.getSubject()).isEqualTo(String.valueOf(JwtTokenFixture.DEFAULT_USER_ID));
		}

		@Test
		@DisplayName("다른 시크릿으로 서명된 토큰이면 예외를 던진다")
		void wrongSignature_throwsException() {
			String token = JwtTokenFixture.createWrongSignatureToken();

			assertThatThrownBy(() -> jwtTokenProvider.parseClaims(token))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Invalid JWT token");
		}

		@Test
		@DisplayName("만료된 토큰이면 예외를 던진다")
		void expiredToken_throwsException() {
			String token = JwtTokenFixture.createExpiredAccessToken();

			assertThatThrownBy(() -> jwtTokenProvider.parseClaims(token))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Invalid JWT token");
		}

		@Test
		@DisplayName("잘못된 형식의 토큰이면 예외를 던진다")
		void malformedToken_throwsException() {
			assertThatThrownBy(() -> jwtTokenProvider.parseClaims("not.a.valid.jwt"))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("클레임 추출 메서드")
	class ClaimExtraction {

		private Claims claims;

		@BeforeEach
		void setUp() {
			String token = JwtTokenFixture.createTokenWithCustomExpiration(
					42L, "ROLE_USER", TokenType.ACCESS,
					JwtTokenFixture.DEFAULT_JTI,
					new Date(System.currentTimeMillis() + 3600_000));
			claims = jwtTokenProvider.parseClaims(token);
		}

		@Test
		@DisplayName("getTokenType - ACCESS 타입을 반환한다")
		void getTokenType_returnsAccess() {
			assertThat(jwtTokenProvider.getTokenType(claims)).isEqualTo(TokenType.ACCESS);
		}

		@Test
		@DisplayName("getUserId - subject에서 userId를 추출한다")
		void getUserId_returnsUserId() {
			assertThat(jwtTokenProvider.getUserId(claims)).isEqualTo(42L);
		}

		@Test
		@DisplayName("getAuthority - auth 클레임을 반환한다")
		void getAuthority_returnsRole() {
			assertThat(jwtTokenProvider.getAuthority(claims)).isEqualTo("ROLE_USER");
		}

		@Test
		@DisplayName("getJti - JTI를 반환한다")
		void getJti_returnsJti() {
			assertThat(jwtTokenProvider.getJti(claims)).isEqualTo(JwtTokenFixture.DEFAULT_JTI);
		}
	}

	@Nested
	@DisplayName("REFRESH 토큰 타입")
	class RefreshToken {

		@Test
		@DisplayName("REFRESH 타입 토큰의 tokenType을 정확히 반환한다")
		void refreshTokenType() {
			String token = JwtTokenFixture.createRefreshToken(JwtTokenFixture.DEFAULT_USER_ID);
			Claims claims = jwtTokenProvider.parseClaims(token);

			assertThat(jwtTokenProvider.getTokenType(claims)).isEqualTo(TokenType.REFRESH);
		}
	}
}