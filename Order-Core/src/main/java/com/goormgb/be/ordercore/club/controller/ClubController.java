package com.goormgb.be.ordercore.club.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.ordercore.club.dto.response.ClubDetailGetResponse;
import com.goormgb.be.ordercore.club.dto.response.ClubGetResponse;
import com.goormgb.be.ordercore.club.service.ClubService;
import com.goormgb.be.ordercore.match.dto.response.ClubMonthlyMatchesResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Club", description = "구단 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {
	private final ClubService clubService;

	@Operation(summary = "구단 전체 조회", description = "KBO 10개 구단 전체 리스트를 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공")
	})
	@GetMapping()
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<ClubGetResponse> getAllClubs() {
		return ApiResult.ok(clubService.getAllClubs());
	}

	@Operation(summary = "구단 상세 조회", description = "구단 ID로 구단 상세 정보 및 현재 시즌 성적을 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "구단을 찾을 수 없음", content = @Content)
	})
	@GetMapping("/{clubId}")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<ClubDetailGetResponse> getClubDetail(
			@Parameter(description = "구단 ID", required = true, example = "1")
			@PathVariable Long clubId
	) {
		return ApiResult.ok(clubService.getClubDetail(clubId));
	}

	@Operation(summary = "구단 월별 경기 일정 조회", description = "특정 구단의 월 단위 홈/원정 경기 일정을 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "구단을 찾을 수 없음", content = @Content)
	})
	@GetMapping("/{clubId}/matches")
	public ApiResult<ClubMonthlyMatchesResponse> getClubMonthlyMatches(
			@Parameter(description = "구단 ID", required = true, example = "1")
			@PathVariable Long clubId,
			@Parameter(description = "조회 연도", required = true, example = "2026")
			@RequestParam int year,
			@Parameter(description = "조회 월 (1~12)", required = true, example = "3")
			@RequestParam int month
	) {
		return ApiResult.ok(clubService.getClubMonthlyMatches(clubId, year, month));
	}

}
