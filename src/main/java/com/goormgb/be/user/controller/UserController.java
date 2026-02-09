package com.goormgb.be.user.controller;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.user.dto.response.OnboardingStatusResponse;
import com.goormgb.be.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    final private UserService userService;

    @Operation(summary = "온보딩 완료 여부 조회", description = "온보딩 선호도 완료 여부를 조회합니다.")
    @GetMapping("/onboarding/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<OnboardingStatusResponse> getOnboardingStatus(
            @AuthenticationPrincipal Long userId
    ) {
        var onboardingStatus = userService.getOnboardingStatus(userId);

        return ApiResult.ok(onboardingStatus);
    }
}
