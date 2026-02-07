package com.goormgb.be.auth.kakao.controller;

import com.goormgb.be.auth.kakao.client.KakaoOAuthClient;
import com.goormgb.be.auth.kakao.dto.KakaoLoginRequest;
import com.goormgb.be.auth.kakao.dto.KakaoLoginResponse;
import com.goormgb.be.auth.kakao.service.KakaoAuthService;
import com.goormgb.be.auth.util.CookieUtils;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.global.support.Preconditions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/kakao")
public class KakaoAuthController {
    private final KakaoAuthService kakaoAuthService;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final CookieUtils cookieUtils;

    @GetMapping("/login-url")
    public ResponseEntity<ApiResult<Map<String, String>>> getKakaoUrl(
            @RequestParam(required = false) String redirectUri)
    {
       /**
        * 프론트엔드가 카카오 로그인 페이지로 이동하기 위한 URL 생성
        * 쿼리로 넘어오면 → 그 값 사용
        * 없으면 → application.yml에 설정된 기본 redirectUri 사용
        */
        String loginUrl = kakaoOAuthClient.createLoginUrl();

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
    @PostMapping("/login")
    public ResponseEntity<ApiResult<KakaoLoginResponse>> kakaoLogin(
        @RequestBody KakaoLoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse

    ){
        Preconditions.validate(
                request.getAuthorizationCode() != null,
                ErrorCode.OAUTH_CODE_REQUEST_FAILED
        );

        // 서비스 로직 실행
        KakaoLoginResponse loginResponse = kakaoAuthService.kakaoLogin(
                request.getAuthorizationCode(), httpRequest
        );

        // Refresh Token Cookie 생성
        String cookie = cookieUtils.createRefreshTokenCookie(loginResponse.getRefreshToken()).toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(ApiResult.ok(loginResponse));


    }


}
