package com.goormgb.be.authguard.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.authguard.auth.dto.DevLoginRequest;
import com.goormgb.be.authguard.auth.dto.TokenRefreshResponse;
import com.goormgb.be.authguard.auth.service.LoadTestAuthService;
import com.goormgb.be.authguard.jwt.util.CookieUtils;
import com.goormgb.be.global.response.ApiResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Load Test Auth", description = "부하테스트용 인증 API (X-Internal-Api-Key 필수)")
@Profile({"local", "dev", "test", "staging"})
@RestController
@RequestMapping("/loadtest")
@RequiredArgsConstructor
public class LoadTestAuthController {

	private final LoadTestAuthService loadTestAuthService;
	private final CookieUtils cookieUtils;

	@Operation(
			summary = "부하테스트 유저 회원가입",
			description = "부하테스트용 유저를 생성합니다. loginId/password 자유 입력 (예: 1/1, a/a)"
	)
	@PostMapping("/signup")
	public ResponseEntity<ApiResult<Void>> signup(
			@Valid @RequestBody DevLoginRequest request
	) {
		loadTestAuthService.signup(request.getLoginId(), request.getPassword());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResult.ok("회원가입 성공", null));
	}

	@Operation(
			summary = "부하테스트 유저 로그인",
			description = "부하테스트 유저로 로그인합니다."
	)
	@PostMapping("/login")
	public ResponseEntity<ApiResult<TokenRefreshResponse>> login(
			@Valid @RequestBody DevLoginRequest request,
			HttpServletRequest httpRequest
	) {
		LoadTestAuthService.LoadTestLoginResult result =
				loadTestAuthService.login(request.getLoginId(), request.getPassword(), httpRequest);

		String cookie = cookieUtils.createRefreshTokenCookie(result.refreshToken()).toString();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie)
				.body(ApiResult.ok("로그인 성공",
						TokenRefreshResponse.of(result.accessToken(), false, false)));
	}
}
