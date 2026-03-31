package com.goormgb.be.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	// A004
	// aop 가로채듯 404로 감싸버리는방법. 500 빼고는 전부다 감싸서 바꿔치기. (hotfix) 퇴근하다 일터졋을떄
	// invalided parameter 하고, 메세지만 바꿔치는 구조 // 온보딩 완료, 이미 완료됨. 이런건 있어야하는데, 파라미터 잘못 입력한건 친절하게 줄 필요가 없다.

	// Common
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다. 백엔드팀에 문의하세요."),
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

	// User
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
	USER_DEACTIVATED(HttpStatus.FORBIDDEN, "비활성화된 사용자입니다."),
	USER_ALREADY_BLOCKED(HttpStatus.CONFLICT, "이미 차단된 사용자입니다."),
	USER_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 활성 상태인 사용자입니다."),

	// Onboarding
	ONBOARDING_NOT_COMPLETED(HttpStatus.FORBIDDEN, "온보딩이 완료되지 않았습니다."),
	ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 온보딩이 완료되었습니다."),
	INVALID_PREFERENCE_RANK(HttpStatus.BAD_REQUEST, "선호 순위는 1~3이어야 합니다."),
	DUPLICATE_PREFERENCE_VIEWPOINT(HttpStatus.BAD_REQUEST, "필수 선호(시야) 항목이 중복됩니다."),
	DUPLICATE_PREFERENCE_SEAT_HEIGHT(HttpStatus.BAD_REQUEST, "필수 선호(좌석 높이) 항목이 중복됩니다."),
	DUPLICATE_PREFERENCE_SECTION(HttpStatus.BAD_REQUEST, "필수 선호(구역) 항목이 중복됩니다."),
	INVALID_PREFERENCE_PRIORITY_VALUE(HttpStatus.BAD_REQUEST, "선호도 우선순위는 1, 2, 3만 가능합니다."),
	INVALID_PRICE_RANGE(HttpStatus.BAD_REQUEST, "최소 가격은 최대 가격보다 클 수 없습니다."),
	MISSING_REQUIRED_PREFERENCE_FIELD(HttpStatus.BAD_REQUEST, "필수 선호 항목이 누락되었습니다."),
	PREFERENCE_NOT_FOUND_FOR_UPDATE(HttpStatus.NOT_FOUND, "수정할 선호도 정보를 찾을 수 없습니다."),
	INVALID_MARKETING_CONSENT(HttpStatus.BAD_REQUEST, "마케팅 동의 정보를 찾을 수 없습니다."),
	INVALID_VIEWPOINT_PRIORITY_COUNT(HttpStatus.BAD_REQUEST, "선호하는 관람 포인트는 최소 1개에서 최대 3개까지 선택해야 합니다."),
	INVALID_VIEWPOINT_PRIORITY_SEQUENCE(HttpStatus.BAD_REQUEST, "선호하는 관람 포인트의 우선순위는 1부터 연속이어야 합니다."),
	INVALID_PREFERRED_BLOCK_COUNT(HttpStatus.BAD_REQUEST, "선호 블럭은 최소 1개에서 최대 10개까지 선택해야 합니다."),
	DUPLICATE_PREFERRED_BLOCK(HttpStatus.BAD_REQUEST, "선호 블럭이 중복됩니다."),
	PREFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "온보딩 선호도 정보를 찾을 수 없습니다."),

	// Auth

	// TODO: 묶을 수 있는 공통 에러사항에 대해서는 한가지 키워드로 묶기. 백엔드 보안처리.

	// HTTP UNAUTHORIZED 401
	// AUTHORIZED_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않습니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "잘못된 인증 정보입니다."),
	REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh Token이 존재하지 않거나 만료, 유효하지 않습니다."),
	REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "Refresh Token이 일치하지 않습니다."),
	INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "잘못된 토큰 타입입니다."),
	BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다."),
	INVALID_INTERNAL_API_KEY(HttpStatus.UNAUTHORIZED, "유효하지 않은 내부 API 키입니다."),
	OAUTH_TOKEN_REQUEST_FAILED(HttpStatus.UNAUTHORIZED, "토큰 발급에 실패했습니다."),
	OAUTH_CODE_REQUEST_FAILED(HttpStatus.BAD_REQUEST, "인가 코드는 필수입니다."),
	OAUTH_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인가 코드가 만료되었거나 이미 사용되었습니다."),
	OAUTH_REDIRECT_URI_MISMATCH(HttpStatus.BAD_REQUEST, "Redirect URI가 일치하지 않습니다."),
	OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "카카오 인증 서버 요청에 실패했습니다."),

	// Club
	CLUB_NOT_FOUND(HttpStatus.NOT_FOUND, "구단을 찾을 수 없습니다."),

	// Match
	MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "경기를 찾을 수 없습니다."),
	MATCH_NOT_AVAILABLE_FOR_QUEUE(HttpStatus.CONFLICT, "현재 대기열 진입이 불가능한 경기입니다."),
	INVALID_MATCH_MONTH(HttpStatus.BAD_REQUEST, "올바른 경기 월을 입력해주세요."),
	INVALID_MATCH_YEAR(HttpStatus.BAD_REQUEST, "올바른 경기 년도를 입력해주세요."),

	// Queue
	QUEUE_ALREADY_ENTERED(HttpStatus.CONFLICT, "이미 대기열에 등록된 사용자입니다."),
	QUEUE_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 경기의 대기열에 등록되어 있지 않습니다."),
	ADMISSION_TOKEN_EXPIRED(HttpStatus.GONE, "입장 가능 시간이 만료되었습니다. 다시 대기열에 진입해주세요."),
	INVALID_TICKET_COUNT(HttpStatus.BAD_REQUEST, "예매 티켓 수가 올바르지 않습니다."),
	INVALID_BOOKING_OPTIONS(HttpStatus.BAD_REQUEST, "예매 옵션이 올바르지 않습니다."),
	INVALID_PROMOTE_COUNT(HttpStatus.BAD_REQUEST, "승급 인원 수가 올바르지 않습니다."),
	QUEUE_PROMOTION_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 대기열 승급 처리가 불가능합니다."),

	// Order
	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
	ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 주문에 접근할 권한이 없습니다."),
	INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "주문 상태가 올바르지 않습니다."),
	ORDER_SEAT_EMPTY(HttpStatus.BAD_REQUEST, "주문 좌석 정보가 없습니다."),

	// Section
	SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "섹션을 찾을 수 없습니다."),

	// Block Recommendation
	NO_AVAILABLE_BLOCK(HttpStatus.NOT_FOUND, "선택하신 선호 블럭 내에서는 현재 해당 연석이 가능한 좌석을 찾지 못했어요."),
	BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "블럭을 찾을 수 없습니다."),

	// Seat Assignment
	NO_CONSECUTIVE_SEAT_AVAILABLE(HttpStatus.NOT_FOUND, "해당 블럭에서 연석 가능한 좌석을 찾을 수 없습니다."),
	SEAT_LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "다른 사용자가 좌석을 선택 중입니다. 잠시 후 다시 시도해주세요."),

	// Seat Hold
	INVALID_SEAT_HOLD_REQUEST(HttpStatus.BAD_REQUEST, "좌석 선점 요청이 올바르지 않습니다."),
	MATCH_SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 경기의 좌석 정보를 찾을 수 없습니다."),
	SEAT_ALREADY_HELD_BY_OTHER(HttpStatus.CONFLICT, "다른 사용자가 이미 좌석을 선점했습니다."),
	SEAT_ALREADY_SOLD(HttpStatus.CONFLICT, "이미 판매 완료된 좌석입니다."),
	SEAT_HOLD_NOT_FOUND(HttpStatus.NOT_FOUND, "좌석 선점 정보를 찾을 수 없습니다."),
	SEAT_HOLD_EXPIRED(HttpStatus.BAD_REQUEST, "좌석 선점이 만료되었습니다."),
	SEAT_HOLD_OWNERSHIP_DENIED(HttpStatus.FORBIDDEN, "해당 좌석 선점에 접근할 권한이 없습니다."),
	PRICE_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 좌석의 가격 정책을 찾을 수 없습니다."),
	SEAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "좌석 세션이 존재하지 않거나 만료되었습니다."),

	// Payment
	PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
	PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 결제가 완료된 주문입니다."),
	CASH_RECEIPT_ALREADY_EXISTS(HttpStatus.CONFLICT, "현금영수증이 이미 신청되었습니다."),
	INVALID_PAYMENT_METHOD(HttpStatus.BAD_REQUEST, "지원하지 않는 결제 수단입니다."),
	BANK_TRANSFER_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "경기 시작 3시간 이내에는 무통장 입금을 이용할 수 없습니다."),

	// Mypage
	INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "size는 최대 10까지 허용됩니다."),
	INVALID_TICKET_TAB(HttpStatus.BAD_REQUEST, "유효하지 않은 탭 값입니다."),
	TICKET_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "취소 가능한 기간이 아닙니다."),
	ENTRY_QR_NOT_AVAILABLE_YET(HttpStatus.BAD_REQUEST, "아직 입장 가능 시간이 아닙니다."),
	ENTRY_QR_MATCH_STARTED(HttpStatus.BAD_REQUEST, "경기 시작 이후에는 QR을 발급할 수 없습니다."),
	INVALID_INQUIRY_CATEGORY(HttpStatus.BAD_REQUEST, "유효하지 않은 문의 카테고리입니다."),
	INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다."),
	INQUIRY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 문의에 접근할 권한이 없습니다."),
	INQUIRY_FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다."),
	INQUIRY_FILE_SIGNATURE_MISMATCH(HttpStatus.BAD_REQUEST, "파일 내용이 확장자와 일치하지 않습니다."),
	INQUIRY_FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "파일 크기 제한을 초과했습니다."),
	INQUIRY_FILE_KEY_INVALID(HttpStatus.BAD_REQUEST, "파일 경로가 유효하지 않습니다."),
	INQUIRY_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "업로드된 파일을 찾을 수 없습니다."),

	;

	private final HttpStatus status;
	private final String message;
}
