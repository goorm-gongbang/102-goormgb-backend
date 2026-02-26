package com.goormgb.be.ordercore.club.controller;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.ordercore.club.dto.response.ClubDetailGetResponse;
import com.goormgb.be.ordercore.club.dto.response.ClubGetResponse;
import com.goormgb.be.ordercore.club.service.ClubService;
import com.goormgb.be.ordercore.match.dto.response.ClubMonthlyMatchesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Club", description = "구단 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {
    private final ClubService clubService;

    @Operation(summary = "구단 전체 조회", description = "구단 전체 리스트를 조회합니다.")
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<ClubGetResponse> getAllClubs() {
        return ApiResult.ok(clubService.getAllClubs());
    }

    @Operation(summary = "구단 상세 조회", description = "구단 상세를 조회합니다.")
    @GetMapping("/{clubId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<ClubDetailGetResponse> getClubDetail(
            @PathVariable Long clubId
    ) {
        return ApiResult.ok(clubService.getClubDetail(clubId));
    }

    @Operation(summary = "구단 경기 일정(월 단위) 조회", description = "구단 월 단위 경기 일정을 조회합니다.")
    @GetMapping("/{clubId}/matches")
    public ApiResult<ClubMonthlyMatchesResponse> getClubMonthlyMatches(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResult.ok(clubService.getClubMonthlyMatches(clubId, year, month));
    }

}
