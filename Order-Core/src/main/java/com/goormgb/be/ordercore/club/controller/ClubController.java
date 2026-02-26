package com.goormgb.be.ordercore.club.controller;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.ordercore.club.dto.response.ClubDetailGetResponse;
import com.goormgb.be.ordercore.club.service.ClubService;
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

    @Operation(summary = "구단 상세 조회", description = "구단 상세를 조회합니다.")
    @GetMapping("/{clubId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<ClubDetailGetResponse> getClubDetail(
            @PathVariable Long clubId
    ) {
        var club = clubService.getClubDetail(clubId);

        return ApiResult.ok(club);
    }
}
