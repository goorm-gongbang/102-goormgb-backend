package com.goormgb.be.authguard.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.authguard.auth.dto.TokenRefreshResponse;
import com.goormgb.be.authguard.auth.dto.WithdrawalResponse;
import com.goormgb.be.authguard.auth.service.AuthService;
import com.goormgb.be.authguard.jwt.util.CookieUtils;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.global.support.Preconditions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final CookieUtils cookieUtils;

	@Operation(summary = "토큰 재발급", description = "HttpOnly 쿠키에 담긴 Refresh Token으로 새로운 Access Token과 Refresh Token을 재발급합니다. 새 Refresh Token은 Set-Cookie 헤더로 반환됩니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
		@ApiResponse(responseCode = "401", description = "Refresh Token 없음 또는 만료", content = @Content)
	})
	@PostMapping("/token/refresh")
	public ResponseEntity<ApiResult<TokenRefreshResponse>> refresh(HttpServletRequest request) {
		// 1. Cookie에서 Refresh Token 추출
		String refreshToken = cookieUtils.extractRefreshToken(request);
		Preconditions.validate(refreshToken != null, ErrorCode.REFRESH_TOKEN_NOT_FOUND);

		// 2. 토큰 재발급
		AuthService.TokenRefreshResult result = authService.refresh(refreshToken, request);

		// 3. 새 Refresh Token을 HttpOnly Cookie로 설정
		String cookie = cookieUtils.createRefreshTokenCookie(result.refreshToken()).toString();

		// 4. 응답
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie)
				.body(ApiResult.ok("토큰 재발급 성공", TokenRefreshResponse.of(result.accessToken())));
	}

	@Operation(summary = "로그아웃", description = "Access Token을 블랙리스트에 등록하고 Refresh Token 쿠키를 삭제합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그아웃 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요 또는 Refresh Token 없음", content = @Content)
	})
	@PostMapping("/logout")
	public ResponseEntity<ApiResult<Void>> logout(HttpServletRequest request) {
		// 1. Refresh Token 추출
		String refreshToken = cookieUtils.extractRefreshToken(request);
		Preconditions.validate(refreshToken != null, ErrorCode.REFRESH_TOKEN_NOT_FOUND);

		// 2. 로그아웃 처리 (Access Token 블랙리스트 등록 + Refresh Token 삭제)
		authService.logout(request, refreshToken);

		// 3. Refresh Token Cookie 삭제
		String cookie = cookieUtils.deleteRefreshTokenCookie().toString();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie)
				.body(ApiResult.ok("로그아웃 성공", null));
	}

	@Operation(summary = "회원 탈퇴 신청", description = "회원 탈퇴를 신청합니다. 즉시 서비스 이용이 중단되고, 30일의 유예 기간 이후 계정이 최종 삭제됩니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "탈퇴 신청 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
	})
	@PostMapping("/withdraw")
	public ResponseEntity<ApiResult<WithdrawalResponse>> withdraw(@AuthenticationPrincipal Long userId) {
		WithdrawalResponse response = authService.withdraw(userId);
		return ResponseEntity.ok()
				.body(ApiResult.ok("탈퇴 처리 완료", response));

	}
}
