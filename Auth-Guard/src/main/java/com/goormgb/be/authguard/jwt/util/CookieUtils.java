package com.goormgb.be.authguard.jwt.util;

import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import com.goormgb.be.authguard.jwt.config.JwtProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CookieUtils {

	public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

	private final JwtProperties jwtProperties;

	/**
	 * Refresh Token용 HttpOnly Cookie 생성 (로그인/토큰 재발급 시)
	 * */
	public ResponseCookie createRefreshTokenCookie(String refreshToken) {
		long maxAgeSeconds = TimeUnit.DAYS.toSeconds(jwtProperties.getRefreshToken().getExpirationDays());

		return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
				.httpOnly(true)
				.secure(true)
				.sameSite("Lax")    // prod 환경
				.path("/")
				.maxAge(maxAgeSeconds)
				.build();
	}

	/**
	 * Refresh Token Cookie 삭제 (로그아웃 시)
	 */
	public ResponseCookie deleteRefreshTokenCookie() {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
				.httpOnly(true)
				.secure(true)
				.sameSite("Lax")    // prod 환경
				.path("/")
				.maxAge(0)    // 즉시 만료 -> 브라우저에서 삭제
				.build();
	}

	/**
	 * Request에서 Refresh Token 추출 (API 요청 시)
	 */
	public String extractRefreshToken(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, REFRESH_TOKEN_COOKIE_NAME);
		return cookie != null ? cookie.getValue() : null;
	}
}
