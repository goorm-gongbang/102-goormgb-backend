package com.goormgb.be.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.auth.dto.DevLoginRequest;
import com.goormgb.be.auth.dto.DevSignupRequest;
import com.goormgb.be.auth.dto.TokenRefreshResponse;
import com.goormgb.be.auth.service.DevAuthService;
import com.goormgb.be.auth.util.CookieUtils;
import com.goormgb.be.global.response.ApiResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Dev Auth", description = "개발용 인증 API (local/dev 프로필 전용)")
@Profile({"local", "dev"})
@RestController
@RequestMapping("/dev/auth")
@RequiredArgsConstructor
public class DevAuthController {

	private final DevAuthService devAuthService;
	private final CookieUtils cookieUtils;

	@Operation(summary = "개발용 회원가입", description = "ID/PW 기반 개발용 계정을 생성합니다.")
	@PostMapping("/signup")
	public ResponseEntity<ApiResult<Void>> signup(@Valid @RequestBody DevSignupRequest request) {
		devAuthService.signup(
				request.getLoginId(),
				request.getPassword(),
				request.getNickname(),
				request.getEmail()
		);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResult.ok("회원가입 성공", null));
	}

	@Operation(summary = "개발용 로그인", description = "ID/PW 기반 개발용 로그인 후 JWT를 발급합니다.")
	@PostMapping("/login")
	public ResponseEntity<ApiResult<TokenRefreshResponse>> login(
			@Valid @RequestBody DevLoginRequest request,
			HttpServletRequest httpRequest
	) {
		DevAuthService.DevLoginResult result = devAuthService.login(
				request.getLoginId(),
				request.getPassword(),
				httpRequest
		);

		String cookie = cookieUtils.createRefreshTokenCookie(result.refreshToken()).toString();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie)
				.body(ApiResult.ok("로그인 성공", TokenRefreshResponse.of(result.accessToken())));
	}
}
