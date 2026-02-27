package com.goormgb.be.ordercore.match.controller;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.ordercore.match.dto.response.MatchDetailGetResponse;
import com.goormgb.be.ordercore.match.dto.response.MatchListByDateResponse;
import com.goormgb.be.ordercore.match.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Match", description = "경기 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/matches")
public class MatchController {
    private final MatchService matchService;

    @Operation(summary = "경기 상세 조회 API", description = "경기 상세를 조회합니다.")
    @GetMapping("/{matchId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<MatchDetailGetResponse> getMatchDetail(
            @PathVariable Long matchId
    ) {
        return ApiResult.ok(matchService.getMatchDetail(matchId));
    }

    @Operation(summary = "날짜 별 경기 조회 API", description = "날짜 별 경기를 조회합니다.")
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<MatchListByDateResponse> getMatchList(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ApiResult.ok(matchService.getMatchesByDate(date));
    }
}
