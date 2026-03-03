package com.goormgb.be.authguard.auth.controller;

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
	void 회원가입_성공() throws Exception {
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
	void 회원가입_로그인아이디_누락() throws Exception {
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
	void 회원가입_패스워드_누락() throws Exception {
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
	void 로그인_성공() throws Exception {
		// given
		DevLoginRequest request = DevLoginRequestFixture.createDefault();
		DevAuthService.DevLoginResult loginResult =
				new DevAuthService.DevLoginResult("access-token-value", "refresh-token-value", false, false);

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
				.andExpect(jsonPath("$.data.accessToken").value("access-token-value"))
				.andExpect(jsonPath("$.data.agreementRequired").value(false))
				.andExpect(jsonPath("$.data.onboardingRequired").value(false));
	}

	@Test
	@DisplayName("POST /dev/auth/login - 비활성화된 계정 로그인 시 403")
	void 로그인_비활성화_계정() throws Exception {
		// given
		DevLoginRequest request = DevLoginRequestFixture.createDefault();

		given(devAuthService.login(eq(request.getLoginId()), eq(request.getPassword()), any(HttpServletRequest.class)))
				.willThrow(new CustomException(ErrorCode.USER_DEACTIVATED));

		// when & then
		mockMvc.perform(post("/dev/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("비활성화된 사용자입니다."));
	}

	@Test
	@DisplayName("POST /dev/auth/login - 잘못된 인증 정보 시 401")
	void 로그인_잘못된_인증_정보() throws Exception {
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
