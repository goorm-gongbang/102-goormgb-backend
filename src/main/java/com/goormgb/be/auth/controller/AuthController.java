package com.goormgb.be.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.auth.dto.TokenRefreshResponse;
import com.goormgb.be.auth.service.AuthService;
import com.goormgb.be.auth.util.CookieUtils;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.global.support.Preconditions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtils cookieUtils;

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access Token과 Refresh Token을 발급합니다.")
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResult<TokenRefreshResponse>> refresh(HttpServletRequest request) {
        // 1. Cookie에서 Refresh Token 추출
        String refreshToken = cookieUtils.extractRefreshToken(request);
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // 2. 토큰 재발급
        AuthService.TokenRefreshResult result = authService.refresh(refreshToken, request);

        // 3. 새 Refresh Token을 HttpOnly Cookie로 설정
        String cookie = cookieUtils.createRefreshTokenCookie(result.refreshToken()).toString();

        // 4. 응답
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(ApiResult.ok("토큰 재발급 성공", TokenRefreshResponse.of(result.accessToken())));
    }

    @Operation(summary = "로그아웃", description = "Access Token을 블랙리스트에 등록하고 Refresh Token을 삭제합니다.")
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
}