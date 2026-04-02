package com.goormgb.be.global.environment;

import org.springframework.http.HttpStatus;

import com.goormgb.be.global.exception.ErrorCode;

/**
 * 프로필별 에러 응답 코드 전략 인터페이스.
 *
 * <p>환경에 따라 클라이언트에 노출되는 에러 코드의 상세 수준을 결정한다.</p>
 *
 * <ul>
 *   <li>{@link DetailedErrorResponseStrategy} (local/dev/test) - ErrorCode enum 이름 그대로 노출 (예: PREFERENCE_NOT_FOUND)</li>
 *   <li>{@link AbstractErrorResponseStrategy} (staging/prod) - HTTP 상태 시리즈로 추상화 (예: CLIENT_ERROR, SERVER_ERROR)</li>
 * </ul>
 *
 * <p>GlobalExceptionHandler에서 CustomException 처리 시 사용된다.</p>
 *
 * @see com.goormgb.be.global.exception.GlobalExceptionHandler
 * @see com.goormgb.be.global.response.ErrorResponse#error(ErrorCode, ErrorResponseStrategy)
 */
public interface ErrorResponseStrategy {
	String resolveCode(ErrorCode errorCode);

	String resolveMessage(ErrorCode errorCode);

	default String resolveCode(HttpStatus status) {
		return status.series().name();
	}

	String resolveMessage(String detailedMessage, HttpStatus status);
}
