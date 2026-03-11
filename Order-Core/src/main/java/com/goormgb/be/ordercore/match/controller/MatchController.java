package com.goormgb.be.ordercore.match.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.ordercore.match.dto.response.MatchDetailGetResponse;
import com.goormgb.be.ordercore.match.dto.response.MatchListByDateResponse;
import com.goormgb.be.ordercore.match.service.MatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Match", description = "경기 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/matches")
public class MatchController {
	private final MatchService matchService;

	@Operation(summary = "경기 상세 조회", description = "경기 ID로 경기 상세 정보를 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "경기를 찾을 수 없음", content = @Content)
	})
	@GetMapping("/{matchId}")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<MatchDetailGetResponse> getMatchDetail(
			@Parameter(description = "경기 ID", required = true, example = "1")
			@PathVariable Long matchId
	) {
		return ApiResult.ok(matchService.getMatchDetail(matchId));
	}

	@Operation(summary = "날짜별 경기 목록 조회", description = "특정 날짜의 전체 경기 목록을 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공")
	})
	@GetMapping()
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<MatchListByDateResponse> getMatchList(
			@Parameter(description = "조회 날짜 (yyyy-MM-dd)", required = true, example = "2026-03-28",
					schema = @Schema(type = "string", format = "date"))
			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate date
	) {
		return ApiResult.ok(matchService.getMatchesByDate(date));
	}
}
