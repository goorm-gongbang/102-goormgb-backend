package com.goormgb.be.authguard.jwt.util;

import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import com.goormgb.be.global.jwt.config.JwtProperties;

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
				.httpOnly(true)     // XSS 방지
				.secure(jwtProperties.getCookie().isSecure())
				.sameSite("Lax")    // CSRF 공격 완화
				.path("/")          // 모든 경로에서 쿠키전송
				.maxAge(maxAgeSeconds)// 7일 만료
				.build();
	}

	/**
	 * Refresh Token Cookie 삭제 (로그아웃 시)
	 */
	public ResponseCookie deleteRefreshTokenCookie() {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
				.httpOnly(true)
				.secure(jwtProperties.getCookie().isSecure())
				.sameSite("Lax")
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
