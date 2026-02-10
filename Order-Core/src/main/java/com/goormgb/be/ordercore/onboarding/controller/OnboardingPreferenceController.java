package com.goormgb.be.ordercore.onboarding.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.ordercore.onboarding.dto.request.OnboardingPreferenceCreateRequest;
import com.goormgb.be.ordercore.onboarding.dto.request.OnboardingPreferenceUpdateRequest;
import com.goormgb.be.ordercore.onboarding.dto.response.OnboardingPreferenceCreateResponse;
import com.goormgb.be.ordercore.onboarding.dto.response.OnboardingPreferenceGetResponse;
import com.goormgb.be.ordercore.onboarding.service.OnboardingPreferenceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Onboarding", description = "온보딩 선호도 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/onboarding")
public class OnboardingPreferenceController {
	final private OnboardingPreferenceService onboardingPreferenceService;

	@Operation(summary = "온보딩 선호도 조회", description = "온보딩 선호도를 조회합니다.")
	@GetMapping("/preferences")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<OnboardingPreferenceGetResponse> getPreferences(
			@AuthenticationPrincipal Long userId
	) {
		var preferences = onboardingPreferenceService.getPreferences(userId);

		return ApiResult.ok(preferences);
	}

	@Operation(summary = "온보딩 선호도 생성", description = "온보딩 선호도를 생성합니다.")
	@PostMapping("/preferences")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResult<OnboardingPreferenceCreateResponse> createPreferences(
			@AuthenticationPrincipal Long userId,
			@RequestBody OnboardingPreferenceCreateRequest request
	) {
		var response = onboardingPreferenceService.createPreferences(userId, request);

		return ApiResult.ok(response);
	}

	@Operation(summary = "온보딩 선호도 수정", description = "온보딩 선호도를 수정합니다.")
	@PutMapping("/preferences")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<Void> updatePreferences(
			@AuthenticationPrincipal Long userId,
			@RequestBody OnboardingPreferenceUpdateRequest request
	) {
		onboardingPreferenceService.updatePreferences(userId, request);

		return ApiResult.ok();
	}
}
