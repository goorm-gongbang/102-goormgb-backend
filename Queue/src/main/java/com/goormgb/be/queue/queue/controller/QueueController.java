package com.goormgb.be.queue.queue.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.queue.queue.dto.request.QueueEnterRequest;
import com.goormgb.be.queue.queue.dto.response.QueueEnterResponse;
import com.goormgb.be.queue.queue.dto.response.QueueStatusResponse;
import com.goormgb.be.queue.queue.enums.QueueStatus;
import com.goormgb.be.queue.queue.service.QueueService;
import com.goormgb.be.queue.queue.util.AdmissionTokenCookieUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Queue", description = "대기열 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@Validated
@RequestMapping("/matches")
@RequiredArgsConstructor
public class QueueController {

	private final QueueService queueService;
	private final AdmissionTokenCookieUtils admissionTokenCookieUtils;

	@Operation(summary = "대기열 진입", description = "사용자의 예매 조건을 저장하고 경기 대기열에 진입시킵니다.")
	@PostMapping("/{matchId}/enter")
	public ApiResult<QueueEnterResponse> enterQueue(
		@PathVariable Long matchId,
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody QueueEnterRequest request
	) {
		return ApiResult.ok("대기열 진입 성공", queueService.enter(matchId, userId, request));
	}

	@Operation(summary = "대기열 상태 조회", description = "현재 사용자의 대기열 상태와 순번을 조회합니다.")
	@GetMapping("/{matchId}/status")
	public ResponseEntity<ApiResult<QueueStatusResponse>> getQueueStatus(
		@PathVariable Long matchId,
		@AuthenticationPrincipal Long userId
	) {
		QueueStatusResponse response = queueService.getStatus(matchId, userId);
		ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

		if (response.status() == QueueStatus.READY && response.admissionToken() != null) {
			builder.header(
				HttpHeaders.SET_COOKIE,
				admissionTokenCookieUtils.createAdmissionTokenCookie(
					response.admissionToken(),
					response.expiresIn()
				).toString()
			);
		} else {
			builder.header(HttpHeaders.SET_COOKIE, admissionTokenCookieUtils.deleteAdmissionTokenCookie().toString());
		}

		return builder.body(ApiResult.ok("대기열 상태 조회 성공", response));
	}
}
