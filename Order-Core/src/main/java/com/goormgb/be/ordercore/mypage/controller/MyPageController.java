package com.goormgb.be.ordercore.mypage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.service.MyPageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "MyPage", description = "마이페이지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

	private final MyPageService myPageService;

	@Operation(
		summary = "마이페이지 프로필 요약 조회",
		description = "사용자 프로필 기본 정보 및 티켓 현황 요약을 반환합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content)
	})
	@GetMapping("/profile")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<MyPageProfileResponse> getProfile(
		@AuthenticationPrincipal Long userId
	) {
		return ApiResult.ok("조회 성공", myPageService.getProfile(userId));
	}

	@Operation(
		summary = "예매 내역 목록 조회",
		description = "사용자의 예매 내역을 탭별로 조회합니다. 상단 요약 정보는 탭과 무관하게 항상 포함됩니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "400", description = "파라미터 오류 (size > 10 등)", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
	})
	@GetMapping("/tickets")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<MyPageTicketListResponse> getTickets(
		@AuthenticationPrincipal Long userId,
		@Parameter(description = "탭 (BOOKED | CANCEL_REFUND)", example = "BOOKED")
		@RequestParam(defaultValue = "BOOKED") String tab,
		@Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
		@RequestParam(defaultValue = "0") int page,
		@Parameter(description = "페이지 크기 (최대 10)", example = "10")
		@RequestParam(defaultValue = "10") int size
	) {
		return ApiResult.ok("조회 성공", myPageService.getTickets(userId, tab, page, size));
	}
}
