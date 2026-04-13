package com.goormgb.be.authguard.auth.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.goormgb.be.authguard.support.WebMvcTestSupport;
import com.goormgb.be.global.environment.ErrorResponseStrategy;
import com.goormgb.be.user.dto.response.UserInfoGetResponse;
import com.goormgb.be.user.enums.UserStatus;
import com.goormgb.be.user.service.UserService;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController 컨트롤러 테스트")
class UserControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private ErrorResponseStrategy errorResponseStrategy;

	private void setAuthentication(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	@DisplayName("GET /me - 온보딩 미완료 유저 조회 성공")
	void 내_정보_조회_온보딩_미완료() throws Exception {
		// given
		Long userId = 1L;
		setAuthentication(userId);

		UserInfoGetResponse response = new UserInfoGetResponse(
			userId, UserStatus.ACTIVATE, "user@example.com", "홍길동", true
		);
		given(userService.getMyInfo(userId)).willReturn(response);

		// when & then
		mockMvc.perform(get("/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.status").value("ACTIVATE"))
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.nickname").value("홍길동"))
			.andExpect(jsonPath("$.data.onboardingRequired").value(true));
	}

	@Test
	@DisplayName("GET /me - 온보딩 완료 유저 조회 성공")
	void 내_정보_조회_온보딩_완료() throws Exception {
		// given
		Long userId = 2L;
		setAuthentication(userId);

		UserInfoGetResponse response = new UserInfoGetResponse(
			userId, UserStatus.ACTIVATE, "user@example.com", "홍길동", false
		);
		given(userService.getMyInfo(userId)).willReturn(response);

		// when & then
		mockMvc.perform(get("/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.onboardingRequired").value(false));
	}
}
