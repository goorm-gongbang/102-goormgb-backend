package com.goormgb.be.authguard.kakao.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.authguard.jwt.util.CookieUtils;
import com.goormgb.be.authguard.kakao.client.KakaoOAuthClient;
import com.goormgb.be.authguard.kakao.dto.KakaoLoginRequest;
import com.goormgb.be.authguard.kakao.dto.KakaoLoginResponse;
import com.goormgb.be.authguard.kakao.service.KakaoAuthService;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.global.support.Preconditions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "Kakao Auth", description = "카카오 OAuth 인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/kakao")
public class KakaoAuthController {
	private final KakaoAuthService kakaoAuthService;
	private final KakaoOAuthClient kakaoOAuthClient;
	private final CookieUtils cookieUtils;

	@Operation(summary = "카카오 로그인 URL 조회", description = "프론트엔드가 카카오 로그인 페이지로 이동하기 위한 URL을 반환합니다. redirectUri를 생략하면 서버에 설정된 기본값을 사용합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그인 URL 반환 성공")
	})
	@GetMapping("/login-url")
	public ResponseEntity<ApiResult<Map<String, String>>> getKakaoUrl(
			@Parameter(description = "카카오 로그인 후 리다이렉트될 URI (미입력 시 서버 기본값 사용)", example = "http://localhost:3000/auth/callback")
			@RequestParam(required = false) String redirectUri) {
		/**
		 * 프론트엔드가 카카오 로그인 페이지로 이동하기 위한 URL 생성
		 * 쿼리로 넘어오면 → 그 값 사용
		 * 없으면 → application.yml에 설정된 기본 redirectUri 사용
		 */
		String loginUrl = kakaoOAuthClient.createLoginUrl(redirectUri);

		return ResponseEntity.ok(
				ApiResult.ok(
						Map.of("loginUrl", loginUrl)
				)
		);

	}

	/**
	 * 카카오 로그인 (회원가입 포함)
	 *
	 * - 프론트 흐름
	 * 1. 프론트가 카카오 로그인 페이지로 리다이렉트
	 * 2. 카카오 로그인 성공
	 * 3. redirect_uri 로 authorizationCode 전달됨
	 * 4. 프론트가 이 authorizationCode를 그대로 백엔드로 전달
	 *
	 * - 이 API 하나로
	 * - 신규 사용자 → 회원가입 + 로그인
	 * - 기존 사용자 → 로그인
	 * 을 모두 처리한다
	 */
	@Operation(summary = "카카오 로그인 / 회원가입", description = """
			카카오 인증 코드(authorizationCode)를 받아 로그인 또는 회원가입을 처리합니다.
			- 신규 회원: 회원가입 + 로그인 → HTTP 201
			- 기존 회원: 로그인 → HTTP 200
			Refresh Token은 HttpOnly 쿠키(Set-Cookie)로 내려갑니다.
			""")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "기존 회원 로그인 성공"),
		@ApiResponse(responseCode = "201", description = "신규 회원 로그인(회원가입) 성공"),
		@ApiResponse(responseCode = "400", description = "인증 코드 누락", content = @Content)
	})
	@PostMapping("/login")
	public ResponseEntity<ApiResult<KakaoLoginResponse>> kakaoLogin(
			@RequestBody KakaoLoginRequest request,
			HttpServletRequest httpRequest

	) {
		Preconditions.validate(
				request.getAuthorizationCode() != null,
				ErrorCode.OAUTH_CODE_REQUEST_FAILED
		);

		// 서비스 로직 실행
		KakaoLoginResponse loginResponse = kakaoAuthService.kakaoLogin(
				request.getAuthorizationCode(), request.getRedirectUri(), httpRequest
		);

		// Refresh Token Cookie 생성
		String cookie = cookieUtils.createRefreshTokenCookie(loginResponse.getRefreshToken()).toString();

		HttpStatus status = loginResponse.isNewUser() ? HttpStatus.CREATED : HttpStatus.OK;

		return ResponseEntity.status(status)
				.header(HttpHeaders.SET_COOKIE, cookie)
				.body(ApiResult.ok(loginResponse));

	}

}
