package com.goormgb.be.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다. 백엔드팀에 문의하세요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
    USER_DEACTIVATED(HttpStatus.FORBIDDEN, "비활성화된 사용자입니다."),

    // Onboarding
    ONBOARDING_NOT_COMPLETED(HttpStatus.FORBIDDEN, "온보딩이 완료되지 않았습니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 온보딩이 완료되었습니다."),
    INVALID_PREFERENCE_RANK(HttpStatus.BAD_REQUEST, "선호 순위는 1~3이어야 합니다."),
    DUPLICATE_PREFERENCE_VIEWPOINT(HttpStatus.BAD_REQUEST,  "필수 선호(시야) 항목이 중복됩니다."),
    DUPLICATE_PREFERENCE_SEAT_HEIGHT(HttpStatus.BAD_REQUEST,  "필수 선호(좌석 높이) 항목이 중복됩니다."),
    DUPLICATE_PREFERENCE_SECTION(HttpStatus.BAD_REQUEST,  "필수 선호(구역) 항목이 중복됩니다."),
    INVALID_PREFERENCE_PRIORITY_VALUE(HttpStatus.BAD_REQUEST,  "선호도 우선순위는 1, 2, 3만 가능합니다."),
    INVALID_PRICE_RANGE(HttpStatus.BAD_REQUEST,  "최소 가격은 최대 가격보다 클 수 없습니다."),
    MISSING_REQUIRED_PREFERENCE_FIELD(HttpStatus.BAD_REQUEST,  "필수 선호 항목이 누락되었습니다."),
    PREFERENCE_NOT_FOUND_FOR_UPDATE(HttpStatus.NOT_FOUND, "수정할 선호도 정보를 찾을 수 없습니다."),
    INVALID_MARKETING_CONSENT(HttpStatus.BAD_REQUEST, "마케팅 동의 정보를 찾을 수 없습니다."),

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "잘못된 인증 정보입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh Token이 존재하지 않거나 만료, 유효하지 않습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "Refresh Token이 일치하지 않습니다."),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "잘못된 토큰 타입입니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다."),
    OAUTH_TOKEN_REQUEST_FAILED(HttpStatus.UNAUTHORIZED, "토큰 발급에 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
