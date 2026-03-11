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
import com.goormgb.be.authguard.auth.dto.DevSignupRequest;
import com.goormgb.be.authguard.auth.dto.TokenRefreshResponse;
import com.goormgb.be.authguard.auth.service.DevAuthService;
import com.goormgb.be.authguard.jwt.util.CookieUtils;
import com.goormgb.be.authguard.metrics.AuthMetricsService;
import com.goormgb.be.global.response.ApiResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Dev Auth", description = "개발용 인증 API (local/dev/test 프로필 전용)")
@Profile({"local", "dev", "test"})
@RestController
@RequestMapping("/dev/auth")
@RequiredArgsConstructor
public class DevAuthController {

	private final DevAuthService devAuthService;
	private final CookieUtils cookieUtils;
	private final AuthMetricsService authMetricsService;

	@Operation(summary = "개발용 회원가입", description = "ID/PW 기반 개발용 계정을 생성합니다. local/dev/test 프로필에서만 사용 가능합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "회원가입 성공"),
		@ApiResponse(responseCode = "400", description = "유효성 검사 실패", content = @Content),
		@ApiResponse(responseCode = "409", description = "이미 존재하는 ID", content = @Content)
	})
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

	@Operation(summary = "개발용 로그인", description = "ID/PW 기반 개발용 로그인 후 Access Token을 반환하고 Refresh Token을 쿠키로 설정합니다. local/dev/test 프로필에서만 사용 가능합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그인 성공"),
		@ApiResponse(responseCode = "400", description = "유효성 검사 실패", content = @Content),
		@ApiResponse(responseCode = "401", description = "잘못된 ID 또는 비밀번호", content = @Content)
	})
	@PostMapping("/login")
	public ResponseEntity<ApiResult<TokenRefreshResponse>> login(
			@Valid @RequestBody DevLoginRequest request,
			HttpServletRequest httpRequest
	) {
		authMetricsService.increaseAuthAttempt();

		DevAuthService.DevLoginResult result = devAuthService.login(
				request.getLoginId(),
				request.getPassword(),
				httpRequest
		);

		String cookie = cookieUtils.createRefreshTokenCookie(result.refreshToken()).toString();

		authMetricsService.increaseAuthSuccess();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie)
				.body(ApiResult.ok("로그인 성공",
						TokenRefreshResponse.of(result.accessToken(), result.agreementRequired(),
								result.onboardingRequired())));
	}
}
