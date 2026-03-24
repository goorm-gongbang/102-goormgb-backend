package com.goormgb.be.ordercore.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.ordercore.order.dto.request.OrderCreateRequest;
import com.goormgb.be.ordercore.order.dto.response.OrderCreateResponse;
import com.goormgb.be.ordercore.order.dto.response.OrderSheetGetResponse;
import com.goormgb.be.ordercore.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage/orders")
public class OrderController {

	private final OrderService orderService;

	@Operation(
		summary = "주문서 조회",
		description = """
			선점된 좌석의 경기·좌석 상세 정보와 성인 기본가(adultPrice)를 조회합니다.
			- 경기장은 잠실야구장으로 고정 반환됩니다.
			- adultPrice는 주중/주말에 따라 자동 계산된 성인 기본가입니다.
			- 할인 가격(장애인, 청소년, 군경 등)은 adultPrice를 기준으로 프론트에서 계산합니다.
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류 또는 선점 만료", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "403", description = "선점 소유권 없음", content = @Content),
		@ApiResponse(responseCode = "404", description = "경기 또는 좌석 선점 없음", content = @Content)
	})
	@GetMapping("/sheet")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<OrderSheetGetResponse> getOrderSheet(
		@AuthenticationPrincipal Long userId,
		@Parameter(description = "경기 ID", required = true, example = "1")
		@RequestParam Long matchId,
		@Parameter(description = "match_seat ID 목록 (콤마 구분)", required = true)
		@RequestParam List<Long> seatIds
	) {
		return ApiResult.ok(orderService.getOrderSheet(userId, matchId, seatIds));
	}

	@Operation(
		summary = "주문 생성",
		description = """
			예매자 정보, 좌석별 티켓 타입·가격, 총 결제 금액을 받아 주문을 생성합니다.
			- seats[].price: 프론트에서 할인 적용한 좌석별 최종 가격
			- totalPrice: 좌석 가격 합계 + 수수료(2,000원) = 총 결제 금액 (프론트 계산)
			- 결제는 목업(무조건 성공)으로 처리됩니다.
			- ticketType: ADULT, YOUTH, MILITARY, CHILD, SENIOR, VETERAN, DISABLED
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "주문 생성 성공"),
		@ApiResponse(responseCode = "400", description = "요청 값 오류 또는 좌석 선점 만료", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "403", description = "선점 소유권 없음", content = @Content),
		@ApiResponse(responseCode = "404", description = "경기 또는 좌석 선점 없음", content = @Content)
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResult<OrderCreateResponse> createOrder(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody OrderCreateRequest request
	) {
		return ApiResult.created(orderService.createOrder(userId, request));
	}
}
