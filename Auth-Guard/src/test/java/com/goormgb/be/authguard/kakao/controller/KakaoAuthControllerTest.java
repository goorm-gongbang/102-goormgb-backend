package com.goormgb.be.authguard.kakao.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.goormgb.be.authguard.fixture.KakaoLoginRequestFixture;
import com.goormgb.be.authguard.kakao.client.KakaoOAuthClient;
import com.goormgb.be.authguard.kakao.dto.KakaoLoginRequest;
import com.goormgb.be.authguard.kakao.dto.KakaoLoginResponse;
import com.goormgb.be.authguard.kakao.service.KakaoAuthService;
import com.goormgb.be.authguard.support.WebMvcTestSupport;
import com.goormgb.be.user.enums.UserStatus;

import jakarta.servlet.http.HttpServletRequest;

@WebMvcTest(controllers = KakaoAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class KakaoAuthControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private KakaoAuthService kakaoAuthService;

	@MockitoBean
	private KakaoOAuthClient kakaoOAuthClient;

	@Test
	@DisplayName("GET /auth/kakao/login-url - 카카오 로그인 URL 조회 성공")
	void 카카오_로그인_URL_조회_성공() throws Exception {
		// given
		String expectedUrl = "https://kauth.kakao.com/oauth/authorize?client_id=test&redirect_uri=http://localhost&response_type=code";
		given(kakaoOAuthClient.createLoginUrl(isNull())).willReturn(expectedUrl);

		// when & then
		mockMvc.perform(get("/kakao/login-url"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.data.loginUrl").value(expectedUrl));
	}

	@Test
	@DisplayName("POST /auth/kakao/login - 카카오 로그인 성공")
	void 카카오_로그인_성공() throws Exception {
		// given
		KakaoLoginRequest request = KakaoLoginRequestFixture.createDefault();

		KakaoLoginResponse loginResponse = KakaoLoginResponse.builder()
				.accessToken("kakao-access-token")
				.refreshToken("kakao-refresh-token")
				.user(KakaoLoginResponse.UserInfo.builder()
						.userId(1L)
						.email("user@kakao.com")
						.nickname("카카오유저")
						.profileImageUrl("https://img.kakao.com/profile.jpg")
						.status(UserStatus.ACTIVATE)
						.build())
				.onboardingRequired(true)
				.build();

		given(kakaoAuthService.kakaoLogin(eq(request.getAuthorizationCode()), any(HttpServletRequest.class)))
				.willReturn(loginResponse);

		ResponseCookie cookie = ResponseCookie.from("refreshToken", "kakao-refresh-token")
				.httpOnly(true).path("/").build();
		given(cookieUtils.createRefreshTokenCookie("kakao-refresh-token")).willReturn(cookie);

		// when & then
		mockMvc.perform(post("/kakao/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(header().exists(HttpHeaders.SET_COOKIE))
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.data.accessToken").value("kakao-access-token"))
				.andExpect(jsonPath("$.data.user.userId").value(1))
				.andExpect(jsonPath("$.data.user.email").value("user@kakao.com"))
				.andExpect(jsonPath("$.data.user.nickname").value("카카오유저"))
				.andExpect(jsonPath("$.data.onboardingRequired").value(true));
	}

	@Test
	@DisplayName("POST /auth/kakao/login - 인가 코드 없으면 400")
	void 카카오_로그인_인가코드_없음() throws Exception {
		// given
		KakaoLoginRequest request = new KakaoLoginRequest();

		// when & then
		mockMvc.perform(post("/kakao/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("인가 코드는 필수입니다."));
	}
}
