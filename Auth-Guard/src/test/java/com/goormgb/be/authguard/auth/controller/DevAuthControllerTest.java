package com.goormgb.be.authguard.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.goormgb.be.authguard.auth.dto.DevLoginRequest;
import com.goormgb.be.authguard.auth.dto.DevSignupRequest;
import com.goormgb.be.authguard.auth.service.DevAuthService;
import com.goormgb.be.authguard.fixture.DevLoginRequestFixture;
import com.goormgb.be.authguard.fixture.DevSignupRequestFixture;
import com.goormgb.be.authguard.metrics.AuthMetricsService;
import com.goormgb.be.authguard.support.WebMvcTestSupport;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;

@WebMvcTest(controllers = DevAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class DevAuthControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private DevAuthService devAuthService;

	@MockitoBean
	private AuthMetricsService authMetricsService;

	@Test
	@DisplayName("POST /dev/auth/signup - 회원가입 성공")
	void signup_성공() throws Exception {
		// given
		DevSignupRequest request = DevSignupRequestFixture.createDefault();

		willDoNothing().given(devAuthService).signup(
			eq(request.getLoginId()),
			eq(request.getPassword()),
			eq(request.getNickname()),
			eq(request.getEmail())
		);

		// when & then
		mockMvc.perform(post("/dev/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.message").value("회원가입 성공"));
	}

	@Test
	@DisplayName("POST /dev/auth/signup - loginId 누락 시 400")
	void signup_loginId_누락() throws Exception {
		// given
		DevSignupRequest request = DevSignupRequestFixture.createDefault();
		org.springframework.test.util.ReflectionTestUtils.setField(request, "loginId", null);

		// when & then
		mockMvc.perform(post("/dev/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST /dev/auth/signup - password 누락 시 400")
	void signup_password_누락() throws Exception {
		// given
		DevSignupRequest request = DevSignupRequestFixture.createDefault();
		org.springframework.test.util.ReflectionTestUtils.setField(request, "password", null);

		// when & then
		mockMvc.perform(post("/dev/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST /dev/auth/login - 로그인 성공")
	void login_성공() throws Exception {
		// given
		DevLoginRequest request = DevLoginRequestFixture.createDefault();
		DevAuthService.DevLoginResult loginResult =
			new DevAuthService.DevLoginResult("access-token-value", "refresh-token-value");

		given(devAuthService.login(eq(request.getLoginId()), eq(request.getPassword()), any(HttpServletRequest.class)))
			.willReturn(loginResult);

		ResponseCookie cookie = ResponseCookie.from("refreshToken", "refresh-token-value")
			.httpOnly(true).path("/").build();
		given(cookieUtils.createRefreshTokenCookie("refresh-token-value")).willReturn(cookie);

		// when & then
		mockMvc.perform(post("/dev/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(header().exists(HttpHeaders.SET_COOKIE))
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.message").value("로그인 성공"))
			.andExpect(jsonPath("$.data.accessToken").value("access-token-value"));
	}

	@Test
	@DisplayName("POST /dev/auth/login - 잘못된 인증 정보 시 401")
	void login_잘못된_인증() throws Exception {
		// given
		DevLoginRequest request = DevLoginRequestFixture.createDefault();

		given(devAuthService.login(eq(request.getLoginId()), eq(request.getPassword()), any(HttpServletRequest.class)))
			.willThrow(new CustomException(ErrorCode.INVALID_CREDENTIALS));

		// when & then
		mockMvc.perform(post("/dev/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("잘못된 인증 정보입니다."));
	}
}
