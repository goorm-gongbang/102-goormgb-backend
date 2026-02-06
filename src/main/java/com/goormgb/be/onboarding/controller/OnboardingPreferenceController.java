package com.goormgb.be.onboarding.controller;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.onboarding.dto.request.OnboardingPreferenceCreateRequest;
import com.goormgb.be.onboarding.dto.request.OnboardingPreferenceUpdateRequest;
import com.goormgb.be.onboarding.dto.response.OnboardingPreferenceCreateResponse;
import com.goormgb.be.onboarding.dto.response.OnboardingPreferenceGetResponse;
import com.goormgb.be.onboarding.service.OnboardingPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/onboaring")
public class OnboardingPreferenceController {
    final private OnboardingPreferenceService onboardingPreferenceService;

    @GetMapping("/preferences")
    public ApiResult<OnboardingPreferenceGetResponse> getPreferences(
            // TODO: token 기반으로 유저 가져오기
            // @AuthenticationPrincipal CurrentUser currentUser
    ) {
        // TODO: currentUser.userId()로 변경
        Long userId = 1L;
        var preferences = onboardingPreferenceService.getPreferences(userId);

        return ApiResult.ok(preferences);
    }

    @PostMapping("/preferences")
    public ApiResult<OnboardingPreferenceCreateResponse> createPreferences(
            // TODO: token 기반으로 유저 가져오기
            // @AuthenticationPrincipal CurrentUser currentUser
            OnboardingPreferenceCreateRequest request
    ) {
        // TODO: currentUser.userId()로 변경
        Long userId = 1L;
        var response = onboardingPreferenceService.createPreferences(userId, request);

        return ApiResult.ok(response);
    }

    @PutMapping("/preferences")
    public ApiResult<Void> updatePreferences(
            // TODO: token 기반으로 유저 가져오기
            // @AuthenticationPrincipal CurrentUser currentUser
            OnboardingPreferenceUpdateRequest request
    ) {
        // TODO: currentUser.userId()로 변경
        Long userId = 1L;
        onboardingPreferenceService.updatePreferences(userId, request);

        return ApiResult.ok();
    }
}
