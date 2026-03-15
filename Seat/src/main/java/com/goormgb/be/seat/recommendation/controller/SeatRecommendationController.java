package com.goormgb.be.seat.recommendation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.seat.recommendation.dto.response.BlockRecommendationResponse;
import com.goormgb.be.seat.recommendation.dto.response.SeatEntryResponse;
import com.goormgb.be.seat.recommendation.service.SeatRecommendationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/matches/{matchId}/recommendations")
public class SeatRecommendationController {

	private final SeatRecommendationService seatRecommendationService;

	@Operation(
		summary = "추천 좌석 초기 조회",
		description = "추천 좌석 페이지에서 진입 초기 정보를 조회합니다.",
		security = @SecurityRequirement(name = "BearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "경기를 찾을 수 없거나 좌석 세션이 존재하지 않거나 만료되었습니다.")
	})
	@GetMapping("/seat-entry")
	public ApiResult<SeatEntryResponse> getRecommendationSeatEntry(
		@PathVariable Long matchId,
		@AuthenticationPrincipal Long userId
		// TODO: 큐 진입 토큰 확인
	) {
		return ApiResult.ok(seatRecommendationService.getRecommendationSeatEntry(matchId, userId));
	}

	@Operation(
		summary = "추천 블럭 리스트 조회",
		description = "사용자의 선호 블럭 내에서 N연석 가능 개수와 취향 점수를 기반으로 추천 블럭 리스트를 반환합니다.",
		security = @SecurityRequirement(name = "BearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "추천 블럭 리스트 조회 성공"),
		@ApiResponse(responseCode = "404", description = "추천 가능한 블럭이 없거나 세션/경기를 찾을 수 없습니다.")
	})
	@GetMapping("/blocks")
	public ApiResult<BlockRecommendationResponse> getRecommendedBlocks(
		@PathVariable Long matchId,
		@AuthenticationPrincipal Long userId
	) {
		return ApiResult.ok(seatRecommendationService.getRecommendedBlocks(matchId, userId));
	}
}
