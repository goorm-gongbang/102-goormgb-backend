package com.goormgb.be.ordercore.mypage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.ordercore.mypage.dto.request.MyPageAccountUpdateRequest;
import com.goormgb.be.ordercore.mypage.dto.request.MyPageInquiryCreateRequest;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageAccountResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryCreateResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketCancelResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketDetailResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketQrResponse;
import com.goormgb.be.ordercore.mypage.service.MyPageInquiryService;
import com.goormgb.be.ordercore.mypage.service.MyPageProfileService;
import com.goormgb.be.ordercore.mypage.service.MyPageTicketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "MyPage", description = "마이페이지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

	private final MyPageProfileService myPageProfileService;
	private final MyPageTicketService myPageTicketService;
	private final MyPageInquiryService myPageInquiryService;

	@Operation(
		summary = "개인정보 조회",
		description = "로그인한 사용자의 계정 기본 정보를 조회합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content)
	})
	@GetMapping("/account")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<MyPageAccountResponse> getAccount(
		@AuthenticationPrincipal Long userId
	) {
		return ApiResult.ok("조회 성공", myPageProfileService.getAccount(userId));
	}

	@Operation(
		summary = "개인정보 수정",
		description = "로그인한 사용자의 닉네임을 수정합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "닉네임 입력값 오류", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content)
	})
	@PutMapping("/account")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<MyPageAccountResponse> updateAccount(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody MyPageAccountUpdateRequest request
	) {
		return ApiResult.ok("수정 성공", myPageProfileService.updateAccount(userId, request));
	}

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
		return ApiResult.ok("조회 성공", myPageProfileService.getProfile(userId));
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
		return ApiResult.ok("조회 성공", myPageTicketService.getTickets(userId, tab, page, size));
	}

	@Operation(
		summary = "예매 상세 조회",
		description = "사용자의 특정 예매(ticketId=orders.id) 상세 정보를 조회합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "403", description = "본인 소유 티켓 아님", content = @Content),
		@ApiResponse(responseCode = "404", description = "티켓(주문) 없음", content = @Content)
	})
	@GetMapping("/tickets/{ticketId}")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<MyPageTicketDetailResponse> getTicketDetail(
		@AuthenticationPrincipal Long userId,
		@PathVariable Long ticketId
	) {
		return ApiResult.ok("조회 성공", myPageTicketService.getTicketDetail(userId, ticketId));
	}

	@Operation(
		summary = "입장용 QR 조회",
		description = "사용자의 특정 예매(ticketId=orders.id)에 대한 입장용 QR 토큰을 조회합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "QR 발급 성공"),
		@ApiResponse(responseCode = "400", description = "발급 불가 상태", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "403", description = "본인 소유 티켓 아님", content = @Content),
		@ApiResponse(responseCode = "404", description = "티켓(주문) 없음", content = @Content)
	})
	@GetMapping("/tickets/{ticketId}/qr")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<MyPageTicketQrResponse> getTicketEntryQr(
		@AuthenticationPrincipal Long userId,
		@PathVariable Long ticketId
	) {
		return ApiResult.ok("QR 발급 성공", myPageTicketService.getTicketEntryQr(userId, ticketId));
	}

	@Operation(
		summary = "티켓 취소 요청",
		description = "사용자의 특정 예매(ticketId=orders.id)에 대한 취소 요청을 처리합니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "취소 요청 완료"),
		@ApiResponse(responseCode = "400", description = "취소 불가 상태", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "403", description = "본인 소유 티켓 아님", content = @Content),
		@ApiResponse(responseCode = "404", description = "티켓(주문) 없음", content = @Content)
	})
	@PostMapping("/tickets/{ticketId}/cancel")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<MyPageTicketCancelResponse> requestTicketCancel(
		@AuthenticationPrincipal Long userId,
		@PathVariable Long ticketId
	) {
		return ApiResult.ok("취소 요청이 완료되었습니다.", myPageTicketService.requestTicketCancel(userId, ticketId));
	}

	@Operation(
		summary = "1:1 문의 작성",
		description = "문의 내용을 등록합니다. 첨부파일은 최대 1개까지 수신 및 검증만 수행하며, 현재 저장하지 않습니다.",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "문의 등록 성공"),
		@ApiResponse(responseCode = "400", description = "요청 값 오류 또는 파일 검증 실패", content = @Content),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
		@ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content),
		@ApiResponse(responseCode = "413", description = "파일 크기 제한 초과", content = @Content)
	})
	@PostMapping(value = "/inquiries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResult<MyPageInquiryCreateResponse> createInquiry(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestPart("inquiry") MyPageInquiryCreateRequest request,
		@RequestPart(value = "file", required = false) MultipartFile file
	) {
		return ApiResult.created("문의가 등록되었습니다.", myPageInquiryService.createInquiry(userId, request, file));
	}
}
