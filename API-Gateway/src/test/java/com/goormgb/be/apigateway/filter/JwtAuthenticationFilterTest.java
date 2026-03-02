package com.goormgb.be.apigateway.filter;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.goormgb.be.apigateway.fixture.JwtTokenFixture;
import com.goormgb.be.apigateway.jwt.config.JwtProperties;
import com.goormgb.be.apigateway.jwt.provider.JwtTokenProvider;
import com.goormgb.be.apigateway.jwt.repository.AccessTokenBlacklistRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private AccessTokenBlacklistRepository blacklistRepository;

	@Mock
	private GatewayFilterChain chain;

	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		JwtProperties properties = new JwtProperties();
		properties.setSecretKey(JwtTokenFixture.SECRET_KEY);
		properties.setIssuer("test-issuer");

		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(properties);
		jwtTokenProvider.init();

		filter = new JwtAuthenticationFilter(jwtTokenProvider, blacklistRepository);
	}

	private MockServerWebExchange createExchange(String path) {
		return MockServerWebExchange.from(
				MockServerHttpRequest.get(path).build());
	}

	private MockServerWebExchange createExchangeWithToken(String path, String token) {
		return MockServerWebExchange.from(
				MockServerHttpRequest.get(path)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.build());
	}

	@Test
	@DisplayName("필터 순서는 -1이다")
	void getOrder_returnsMinusOne() {
		assertThat(filter.getOrder()).isEqualTo(-1);
	}

	@Nested
	@DisplayName("화이트리스트 경로")
	class WhitelistedPaths {

		@ParameterizedTest
		@ValueSource(strings = {
				"/auth/kakao",
				"/auth/kakao/callback",
				"/auth/token/refresh",
				"/auth/dev/auth",
				"/auth/dev/auth/login",
				"/swagger-ui",
				"/swagger-ui/index.html",
				"/v3/api-docs",
				"/v3/api-docs/swagger-config",
				"/auth/v3/api-docs",
				"/queue/v3/api-docs",
				"/seat/v3/api-docs",
				"/order/v3/api-docs",
				"/recommendation/v3/api-docs",
				"/actuator",
				"/actuator/health",
				"/actuator/prometheus",
				"/order/clubs",
				"/order/clubs/1",
				"/order/clubs/1/matches",
				"/order/matches",
				"/order/matches/1"
		})
		@DisplayName("화이트리스트 경로는 인증 없이 통과한다")
		void whitelistedPath_passesWithoutAuth(String path) {
			MockServerWebExchange exchange = createExchange(path);
			when(chain.filter(any())).thenReturn(Mono.empty());

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			verify(chain).filter(any());
			verify(blacklistRepository, never()).isBlacklisted(anyString());
		}
	}

	@Nested
	@DisplayName("토큰이 없는 경우")
	class NoToken {

		@Test
		@DisplayName("Authorization 헤더가 없으면 401을 반환한다")
		void noAuthorizationHeader_returns401() {
			MockServerWebExchange exchange = createExchange("/order/onboarding/preferences");

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
			verify(chain, never()).filter(any());
		}

		@Test
		@DisplayName("Bearer 접두사가 없으면 401을 반환한다")
		void noBearerPrefix_returns401() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/order/onboarding/preferences")
							.header(HttpHeaders.AUTHORIZATION, "Basic some-token")
							.build());

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
			verify(chain, never()).filter(any());
		}
	}

	@Nested
	@DisplayName("유효한 ACCESS 토큰")
	class ValidAccessToken {

		@Test
		@DisplayName("유효한 토큰이면 X-User-Id, X-User-Role 헤더를 추가하고 통과한다")
		void validToken_addsHeadersAndPasses() {
			String jti = UUID.randomUUID().toString();
			String token = JwtTokenFixture.createAccessToken(42L, "ROLE_USER", jti);
			MockServerWebExchange exchange = createExchangeWithToken("/order/onboarding/preferences", token);

			when(blacklistRepository.isBlacklisted(jti)).thenReturn(Mono.just(false));

			ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
			when(chain.filter(captor.capture())).thenReturn(Mono.empty());

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			ServerWebExchange capturedExchange = captor.getValue();
			HttpHeaders headers = capturedExchange.getRequest().getHeaders();
			assertThat(headers.getFirst("X-User-Id")).isEqualTo("42");
			assertThat(headers.getFirst("X-User-Role")).isEqualTo("ROLE_USER");
		}

		@Test
		@DisplayName("다른 userId도 정확히 헤더에 전달된다")
		void differentUserId_isPassedCorrectly() {
			String jti = UUID.randomUUID().toString();
			String token = JwtTokenFixture.createAccessToken(99L, "ROLE_USER", jti);
			MockServerWebExchange exchange = createExchangeWithToken("/seat/available", token);

			when(blacklistRepository.isBlacklisted(jti)).thenReturn(Mono.just(false));

			ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
			when(chain.filter(captor.capture())).thenReturn(Mono.empty());

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			ServerWebExchange capturedExchange = captor.getValue();
			assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("99");
			assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_USER");
		}
	}

	@Nested
	@DisplayName("블랙리스트 토큰")
	class BlacklistedToken {

		@Test
		@DisplayName("블랙리스트에 등록된 토큰이면 401을 반환한다")
		void blacklistedToken_returns401() {
			String jti = "blacklisted-jti";
			String token = JwtTokenFixture.createAccessTokenWithJti(jti);
			MockServerWebExchange exchange = createExchangeWithToken("/order/onboarding/preferences", token);

			when(blacklistRepository.isBlacklisted(jti)).thenReturn(Mono.just(true));

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
			verify(chain, never()).filter(any());
		}
	}

	@Nested
	@DisplayName("잘못된 토큰")
	class InvalidToken {

		@Test
		@DisplayName("잘못된 서명의 토큰이면 401을 반환한다")
		void invalidSignature_returns401() {
			String token = JwtTokenFixture.createWrongSignatureToken();
			MockServerWebExchange exchange = createExchangeWithToken("/order/onboarding/preferences", token);

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
			verify(chain, never()).filter(any());
		}

		@Test
		@DisplayName("만료된 토큰이면 401을 반환한다")
		void expiredToken_returns401() {
			String token = JwtTokenFixture.createExpiredAccessToken();
			MockServerWebExchange exchange = createExchangeWithToken("/order/onboarding/preferences", token);

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
			verify(chain, never()).filter(any());
		}

		@Test
		@DisplayName("형식이 잘못된 토큰이면 401을 반환한다")
		void malformedToken_returns401() {
			MockServerWebExchange exchange = createExchangeWithToken("/order/onboarding/preferences", "not-a-jwt");

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
			verify(chain, never()).filter(any());
		}
	}

	@Nested
	@DisplayName("REFRESH 토큰")
	class RefreshTokenUsed {

		@Test
		@DisplayName("REFRESH 타입 토큰이면 401을 반환한다")
		void refreshToken_returns401() {
			String token = JwtTokenFixture.createRefreshToken(JwtTokenFixture.DEFAULT_USER_ID);
			MockServerWebExchange exchange = createExchangeWithToken("/order/onboarding/preferences", token);

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
			verify(chain, never()).filter(any());
			verify(blacklistRepository, never()).isBlacklisted(anyString());
		}
	}
}