package com.goormgb.be.seat.common.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.seat.common.dto.response.SeatGroupsEntryResponse;
import com.goormgb.be.seat.common.service.SeatCommonService;

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
}
