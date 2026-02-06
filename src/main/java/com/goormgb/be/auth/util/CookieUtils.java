package com.goormgb.be.auth.util;

import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.goormgb.be.auth.config.JwtProperties;

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
				.secure(false)      // HTTPS만 허용 꺼놓음. 로컬 개발용 (운영 시 true로 변경)
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
				.secure(false)
				.sameSite("Lax")
				.path("/")
				.maxAge(0)    // 즉시 만료 -> 브라우저에서 삭제
				.build();
	}

	/**
	 * Request에서 Refresh Token 추출 (API 요청 시)
	 */
	public String extractRefreshToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
