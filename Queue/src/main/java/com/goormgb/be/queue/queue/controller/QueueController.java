package com.goormgb.be.queue.queue.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.queue.config.QueueProperties;
import com.goormgb.be.queue.queue.dto.response.QueueEnterResponse;
import com.goormgb.be.queue.queue.dto.response.QueueStatusResponse;
import com.goormgb.be.queue.queue.enums.QueueStatus;
import com.goormgb.be.queue.queue.service.QueueService;
import com.goormgb.be.queue.queue.util.AdmissionTokenCookieUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Queue", description = "대기열 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class QueueController {

	private final QueueService queueService;
	private final AdmissionTokenCookieUtils admissionTokenCookieUtils;
	private final QueueProperties queueProperties;

	@Operation(summary = "대기열 진입", description = "경기 대기열에 진입합니다.")
	@PostMapping("/{matchId}/enter")
	public ApiResult<QueueEnterResponse> enterQueue(
		@PathVariable Long matchId,
		@AuthenticationPrincipal Long userId
	) {
		return ApiResult.ok("대기열 진입 성공", queueService.enter(matchId, userId));
	}

	@Operation(summary = "대기열 상태 조회", description = "현재 사용자의 대기열 상태와 순번을 조회합니다.")
	@GetMapping("/{matchId}/status")
	public ResponseEntity<ApiResult<QueueStatusResponse>> getQueueStatus(
		@PathVariable Long matchId,
		@AuthenticationPrincipal Long userId
	) {
		QueueStatusResponse response = queueService.getStatus(matchId, userId);
		QueueStatusResponse responseBody = new QueueStatusResponse(
			response.status(),
			response.rank(),
			response.totalWaitingCount(),
			null,
			response.expiresIn(),
			response.pollingMs()
		);
		ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

		if (response.status() == QueueStatus.READY && response.admissionToken() != null) {
			builder.header(
				HttpHeaders.SET_COOKIE,
				admissionTokenCookieUtils.createAdmissionTokenCookie(
					response.admissionToken(),
					queueProperties.admissionTtlSeconds()
				).toString()
			);
		} else {
			builder.header(HttpHeaders.SET_COOKIE, admissionTokenCookieUtils.deleteAdmissionTokenCookie().toString());
		}

		return builder.body(ApiResult.ok("대기열 상태 조회 성공", responseBody));
	}

	@Deprecated
	@Operation(summary = "대기열 이탈", description = "현재 경기의 WAITING 또는 READY 상태를 포기하고 대기열에서 이탈합니다.", deprecated = true)
	@DeleteMapping("/{matchId}/enter")
	public ResponseEntity<ApiResult<Void>> leaveQueue(
		@PathVariable Long matchId,
		@AuthenticationPrincipal Long userId
	) {
		queueService.leave(matchId, userId);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, admissionTokenCookieUtils.deleteAdmissionTokenCookie().toString())
			.body(ApiResult.ok("대기열 이탈 성공", null));
	}
}
