package com.goormgb.be.authguard.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.goormgb.be.authguard.auth.dto.WithdrawalResponse;
import com.goormgb.be.authguard.auth.service.AuthService;
import com.goormgb.be.authguard.support.WebMvcTestSupport;

import jakarta.servlet.http.HttpServletRequest;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private AuthService authService;

	private void setAuthentication(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(userId, null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	@DisplayName("POST /auth/token/refresh - 토큰 재발급 성공")
	void 토큰_재발급_성공() throws Exception {
		// given
		String oldRefreshToken = "old-refresh-token";
		AuthService.TokenRefreshResult result =
				new AuthService.TokenRefreshResult("new-access-token", "new-refresh-token");

		given(cookieUtils.extractRefreshToken(any(HttpServletRequest.class))).willReturn(oldRefreshToken);
		given(authService.refresh(eq(oldRefreshToken), any(HttpServletRequest.class))).willReturn(result);

		ResponseCookie cookie = ResponseCookie.from("refreshToken", "new-refresh-token")
				.httpOnly(true).path("/").build();
		given(cookieUtils.createRefreshTokenCookie("new-refresh-token")).willReturn(cookie);

		// when & then
		mockMvc.perform(post("/token/refresh"))
				.andExpect(status().isOk())
				.andExpect(header().exists(HttpHeaders.SET_COOKIE))
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("토큰 재발급 성공"))
				.andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
	}

	@Test
	@DisplayName("POST /auth/token/refresh - 쿠키 없으면 401")
	void 토큰_재발급_리프레시_쿠키_없음() throws Exception {
		// given
		given(cookieUtils.extractRefreshToken(any(HttpServletRequest.class))).willReturn(null);

		// when & then
		mockMvc.perform(post("/token/refresh"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Refresh Token이 존재하지 않거나 만료, 유효하지 않습니다."));
	}

	@Test
	@DisplayName("POST /auth/logout - 로그아웃 성공")
	void 로그아웃_성공() throws Exception {
		// given
		String refreshToken = "refresh-token";

		given(cookieUtils.extractRefreshToken(any(HttpServletRequest.class))).willReturn(refreshToken);
		willDoNothing().given(authService).logout(any(HttpServletRequest.class), eq(refreshToken));

		ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
				.httpOnly(true).path("/").maxAge(0).build();
		given(cookieUtils.deleteRefreshTokenCookie()).willReturn(deleteCookie);

		// when & then
		mockMvc.perform(post("/logout"))
				.andExpect(status().isOk())
				.andExpect(header().exists(HttpHeaders.SET_COOKIE))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")))
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("로그아웃 성공"));
	}

	@Test
	@DisplayName("POST /auth/logout - 쿠키 없으면 401")
	void 로그아웃_리프레시_쿠키_없음() throws Exception {
		// given
		given(cookieUtils.extractRefreshToken(any(HttpServletRequest.class))).willReturn(null);

		// when & then
		mockMvc.perform(post("/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Refresh Token이 존재하지 않거나 만료, 유효하지 않습니다."));
	}

	@Test
	@DisplayName("POST /auth/withdraw - 회원 탈퇴 성공")
	void 회원_탈퇴_성공() throws Exception {
		// given
		Long userId = 1L;
		setAuthentication(userId);

		Instant withdrawnAt = LocalDateTime.of(2026, 2, 17, 12, 0, 0).toInstant(ZoneOffset.UTC);
		Instant reactivateUntil = withdrawnAt.plus(30, ChronoUnit.DAYS);
		WithdrawalResponse response = new WithdrawalResponse("DEACTIVATE", withdrawnAt, reactivateUntil);

		given(authService.withdraw(userId)).willReturn(response);

		// when & then
		mockMvc.perform(post("/withdraw"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("탈퇴 처리 완료"))
				.andExpect(jsonPath("$.data.status").value("DEACTIVATE"))
				.andExpect(jsonPath("$.data.withdrawnAt").exists())
				.andExpect(jsonPath("$.data.reactivateUntil").exists());
	}
}
