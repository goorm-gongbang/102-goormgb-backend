package com.goormgb.be.queue.queue.util;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AdmissionTokenCookieUtils {

	public static final String ADMISSION_TOKEN_COOKIE_NAME = "admissionToken";

	public ResponseCookie createAdmissionTokenCookie(String admissionToken, long maxAgeSeconds) {
		return ResponseCookie.from(ADMISSION_TOKEN_COOKIE_NAME, admissionToken)
			.httpOnly(true)
			.secure(false)
			.sameSite("Lax")
			.path("/")
			.maxAge(maxAgeSeconds)
			.build();
	}

	public ResponseCookie deleteAdmissionTokenCookie() {
		return ResponseCookie.from(ADMISSION_TOKEN_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(false)
			.sameSite("Lax")
			.path("/")
			.maxAge(0)
			.build();
	}
}
