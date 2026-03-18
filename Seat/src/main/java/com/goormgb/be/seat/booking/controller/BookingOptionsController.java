package com.goormgb.be.seat.booking.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.seat.booking.dto.request.BookingOptionsRequest;
import com.goormgb.be.seat.booking.dto.response.BookingOptionsResponse;
import com.goormgb.be.seat.booking.service.BookingOptionsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Booking Options", description = "예매 옵션 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/matches/{matchId}")
public class BookingOptionsController {

	private final BookingOptionsService bookingOptionsService;

	@Operation(
		summary = "예매 옵션 저장",
		description = "대기열 진입 전 예매 옵션(추천 여부, 인원수, 준연석 허용)을 저장합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "저장 성공"),
		@ApiResponse(responseCode = "404", description = "경기를 찾을 수 없습니다.")
	})
	@PostMapping("/booking-options")
	public ApiResult<BookingOptionsResponse> saveBookingOptions(
		@PathVariable Long matchId,
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody BookingOptionsRequest request
	) {
		return ApiResult.ok(bookingOptionsService.saveBookingOptions(matchId, userId, request));
	}
}
