package com.goormgb.be.ordercore.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.ordercore.payment.dto.request.CashReceiptCreateRequest;
import com.goormgb.be.ordercore.payment.dto.request.PaymentProcessRequest;
import com.goormgb.be.ordercore.payment.dto.response.CashReceiptCreateResponse;
import com.goormgb.be.ordercore.payment.dto.response.PaymentProcessResponse;
import com.goormgb.be.ordercore.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Payment", description = "결제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage/orders")
public class PaymentController {

	private final PaymentService paymentService;

	@Operation(
		summary = "결제 처리",
		description = """
			결제 수단을 선택하여 결제를 진행합니다.
			- VIRTUAL_ACCOUNT: 가상계좌 발급, 입금 대기 상태 유지
			- TOSS_PAY / KAKAO_PAY: 즉시 결제 완료 처리 (목업)
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "결제 처리 성공"),
		@ApiResponse(responseCode = "400", description = "이미 결제 완료 또는 잘못된 요청", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "403", description = "주문 소유권 없음", content = @Content),
		@ApiResponse(responseCode = "404", description = "주문 없음", content = @Content)
	})
	@PostMapping("/{orderId}/payment")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<PaymentProcessResponse> processPayment(
		@AuthenticationPrincipal Long userId,
		@Parameter(description = "주문 ID", required = true, example = "1")
		@PathVariable Long orderId,
		@Valid @RequestBody PaymentProcessRequest request
	) {
		return ApiResult.ok(paymentService.processPayment(userId, orderId, request));
	}

	@Operation(
		summary = "현금영수증 신청",
		description = "결제 완료된 주문에 현금영수증을 신청합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "현금영수증 신청 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "403", description = "주문 소유권 없음", content = @Content),
		@ApiResponse(responseCode = "404", description = "주문 또는 결제 정보 없음", content = @Content),
		@ApiResponse(responseCode = "409", description = "현금영수증 이미 신청됨", content = @Content)
	})
	@PostMapping("/{orderId}/cash-receipt")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResult<CashReceiptCreateResponse> createCashReceipt(
		@AuthenticationPrincipal Long userId,
		@Parameter(description = "주문 ID", required = true, example = "1")
		@PathVariable Long orderId,
		@Valid @RequestBody CashReceiptCreateRequest request
	) {
		return ApiResult.ok(paymentService.createCashReceipt(userId, orderId, request));
	}
}
