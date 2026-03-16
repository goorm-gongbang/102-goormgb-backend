package com.goormgb.be.seat.common.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.seat.common.dto.request.SeatHoldCreateRequest;
import com.goormgb.be.seat.common.dto.response.SeatGroupsEntryResponse;
import com.goormgb.be.seat.common.dto.response.SeatHoldCreateResponse;
import com.goormgb.be.seat.common.dto.response.SectionBlocksResponse;
import com.goormgb.be.seat.common.service.SeatCommonService;
import com.goormgb.be.seat.common.service.SeatHoldService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/matches/{matchId}")
public class SeatCommonController {

	private final SeatCommonService seatCommonService;
	private final SeatHoldService seatHoldService;

	@Operation(
		summary = "좌석 그룹 초기 조회",
		description = "좌석 선택 페이지 진입 시 필요한 초기 정보를 조회합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "경기를 찾을 수 없거나 좌석 세션이 존재하지 않거나 만료되었습니다.")
	})
	@GetMapping("/seat-groups")
	public ApiResult<SeatGroupsEntryResponse> getCommonSeatGroup(
		@PathVariable Long matchId,
		@AuthenticationPrincipal Long userId
		// TODO: 큐 진입 토큰 확인
	) {
		return ApiResult.ok(seatCommonService.getSeatGroupsEntry(matchId, userId));
	}

	@Operation(
		summary = "직접 선택 좌석 선점",
		description = "사용자가 선택한 좌석을 5분간 선점(Hold)합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "좌석 선점 성공"),
		@ApiResponse(responseCode = "400", description = "좌석 요청 값이 유효하지 않습니다."),
		@ApiResponse(responseCode = "404", description = "좌석 또는 좌석 세션을 찾을 수 없습니다."),
		@ApiResponse(responseCode = "409", description = "다른 사용자가 이미 좌석을 선점 중입니다.")
	})
	@PostMapping("/seat-holds")
	public ApiResult<SeatHoldCreateResponse> createSeatHolds(
		@PathVariable Long matchId,
		@RequestBody SeatHoldCreateRequest request,
		@AuthenticationPrincipal Long userId
	) {
		return ApiResult.ok(seatHoldService.createOrRefreshHold(userId, matchId, request.seatIds()));
	}

	@Operation(
		summary = "섹션 별 블럭 좌석 현황 조회",
		description = "특정 경기/섹션의 블럭별 좌석 현황을 행 단위로 조회합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "경기, 섹션을 찾을 수 없거나 좌석 세션이 존재하지 않거나 만료되었습니다.")
	})
	@GetMapping("/sections/{sectionId}/blocks")
	public ApiResult<SectionBlocksResponse> getSectionBlocks(
		@PathVariable Long matchId,
		@PathVariable Long sectionId,
		@AuthenticationPrincipal Long userId
		// TODO: 큐 진입 토큰 확인
	) {
		return ApiResult.ok(seatCommonService.getSectionBlocks(matchId, sectionId, userId));
	}

}
