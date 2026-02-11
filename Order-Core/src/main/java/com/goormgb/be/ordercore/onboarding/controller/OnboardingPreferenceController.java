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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					content = @Content(mediaType = "application/json",
							examples = @ExampleObject(value = """
									{
									  "marketingConsent": {
									    "marketingAgreed": true
									  },
									  "preferences": [
									    {
									      "priority": 1,
									      "viewpoint": "CENTER",
									      "seatHeight": "LOW",
									      "section": "MIDDLE",
									      "seatPositionPref": "AISLE",
									      "environmentPref": "SHADE",
									      "moodPref": "CHEERFUL",
									      "obstructionSensitivity": "NET_SENSITIVE",
									      "priceMode": "ANY"
									    },
									    {
									      "priority": 2,
									      "viewpoint": "INFIELD_1B",
									      "seatHeight": "MID",
									      "section": "CENTER_SIDE",
									      "seatPositionPref": "ANY",
									      "environmentPref": "ANY",
									      "moodPref": "ANY",
									      "obstructionSensitivity": "NORMAL",
									      "priceMode": "ANY"
									    },
									    {
									      "priority": 3,
									      "viewpoint": "OUTFIELD_L",
									      "seatHeight": "HIGH",
									      "section": "CORNER",
									      "seatPositionPref": "ANY",
									      "environmentPref": "ANY",
									      "moodPref": "ANY",
									      "obstructionSensitivity": "ANY",
									      "priceMode": "ANY"
									    }
									  ]
									}
									""")))
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
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					content = @Content(mediaType = "application/json",
							examples = @ExampleObject(value = """
									{
									  "preferences": [
									    {
									      "priority": 1,
									      "viewpoint": "INFIELD_3B",
									      "seatHeight": "LOW",
									      "section": "CENTER_SIDE",
									      "seatPositionPref": "ANY",
									      "environmentPref": "ANY",
									      "moodPref": "QUIET",
									      "obstructionSensitivity": "NORMAL",
									      "priceMode": "RANGE",
									      "priceMin": 15000,
									      "priceMax": 40000
									    },
									    {
									      "priority": 2,
									      "viewpoint": "CENTER",
									      "seatHeight": "MID",
									      "section": "MIDDLE",
									      "seatPositionPref": "AISLE",
									      "environmentPref": "SHADE",
									      "moodPref": "CHEERFUL",
									      "obstructionSensitivity": "NET_SENSITIVE",
									      "priceMode": "ANY"
									    },
									    {
									      "priority": 3,
									      "viewpoint": "OUTFIELD_R",
									      "seatHeight": "ANY",
									      "section": "CORNER",
									      "seatPositionPref": "ANY",
									      "environmentPref": "ANY",
									      "moodPref": "ANY",
									      "obstructionSensitivity": "ANY",
									      "priceMode": "ANY"
									    }
									  ]
									}
									""")))
			@RequestBody OnboardingPreferenceUpdateRequest request
	) {
		onboardingPreferenceService.updatePreferences(userId, request);

		return ApiResult.ok();
	}
}
