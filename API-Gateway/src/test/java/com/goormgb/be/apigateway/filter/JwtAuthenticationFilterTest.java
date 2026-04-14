package com.goormgb.be.apigateway.filter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
		properties.setPublicKey(JwtTokenFixture.PUBLIC_KEY_BASE64);
		properties.setIssuer("test-issuer");

		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(properties);
		jwtTokenProvider.init();

		filter = new JwtAuthenticationFilter(jwtTokenProvider, blacklistRepository);
	}

	private MockServerWebExchange createExchange(String path) {
		return MockServerWebExchange.from(
				MockServerHttpRequest.get(path).build());
	}

	private MockServerWebExchange createExchange(HttpMethod method, String path) {
		return MockServerWebExchange.from(
				MockServerHttpRequest.method(method, path).build());
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
	@DisplayName("화이트리스트 경로 + 허용 메서드")
	class WhitelistedPaths {

		@ParameterizedTest
		@CsvSource({
				// 인증 엔드포인트 — POST 전용
				"POST, /auth/kakao",
				"POST, /auth/kakao/callback",
				"POST, /auth/token/refresh",
				"POST, /auth/dev/auth",
				"POST, /auth/dev/auth/login",
				// Swagger / OpenAPI 문서 — GET 전용
				"GET, /swagger-ui",
				"GET, /swagger-ui/index.html",
				"GET, /v3/api-docs",
				"GET, /v3/api-docs/swagger-config",
				"GET, /auth/v3/api-docs",
				"GET, /queue/v3/api-docs",
				"GET, /seat/v3/api-docs",
				"GET, /order/v3/api-docs",
				"GET, /recommendation/v3/api-docs",
				// Actuator 공개 엔드포인트 — GET 전용
				"GET, /actuator/health",
				"GET, /actuator/prometheus",
				// 공개 조회 API — GET 전용
				"GET, /seat/blocks",
				"GET, /order/clubs",
				"GET, /order/clubs/1",
				"GET, /order/clubs/1/matches",
				"GET, /order/matches",
				"GET, /order/matches/1"
		})
		@DisplayName("허용 메서드 + 화이트리스트 경로는 인증 없이 통과한다")
		void whitelistedPath_passesWithoutAuth(HttpMethod method, String path) {
			MockServerWebExchange exchange = createExchange(method, path);
			when(chain.filter(any())).thenReturn(Mono.empty());

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			verify(chain).filter(any());
			verify(blacklistRepository, never()).isBlacklisted(anyString());
		}

		@ParameterizedTest
		@CsvSource({
				// 공개 조회 API에 쓰기 메서드 → 401
				"POST, /order/clubs",
				"PUT, /order/clubs",
				"PATCH, /order/clubs",
				"DELETE, /order/clubs",
				"POST, /order/matches",
				"DELETE, /order/matches",
				"POST, /seat/blocks",
				"PUT, /seat/blocks",
				"DELETE, /seat/blocks",
				// POST 전용 엔드포인트에 GET → 401
				"GET, /auth/kakao",
				"GET, /auth/token/refresh",
				// Swagger 경로에 쓰기 메서드 → 401
				"POST, /v3/api-docs",
				"DELETE, /swagger-ui",
				// Actuator health에 쓰기 메서드 → 401
				"POST, /actuator/health",
				"DELETE, /actuator/prometheus"
		})
		@DisplayName("화이트리스트 경로라도 허용되지 않은 메서드는 인증 우회가 차단된다")
		void whitelistedPath_wrongMethod_isBlocked(HttpMethod method, String path) {
			MockServerWebExchange exchange = createExchange(method, path);

			StepVerifier.create(filter.filter(exchange, chain))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
			verify(chain, never()).filter(any());
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
