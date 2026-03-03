package com.goormgb.be.global.exception;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.goormgb.be.global.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse.ErrorData> handleException(Exception e) {
		log.error(e.getMessage(), e);
		return ErrorResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다. 백엔드팀에 문의하세요.");
	}

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse.ErrorData> handleCustomException(CustomException e) {
		return ErrorResponse.error(e.getErrorCode().getStatus(), e.getErrorCode().getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse.ErrorData> handleValidationException(MethodArgumentNotValidException e) {
		var details = Arrays.toString(e.getDetailMessageArguments());
		var message = details.split(",", 2)[1].replace("]", "").trim();
		return ErrorResponse.error(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<ErrorResponse.ErrorData> handleAuthorizationDenied(AuthorizationDeniedException e) {
		return ErrorResponse.error(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse.ErrorData> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
		log.warn("JSON parse error: {}", e.getMessage());
		return ErrorResponse.error(HttpStatus.BAD_REQUEST, "요청 본문의 JSON 형식이 올바르지 않습니다.");
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse.ErrorData> handleMissingParam(MissingServletRequestParameterException e) {
		String message = String.format("필수 파라미터 '%s'이(가) 누락되었습니다.", e.getParameterName());
		return ErrorResponse.error(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse.ErrorData> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		if (e.getRequiredType() != null && e.getRequiredType().equals(java.time.LocalDate.class)) {
			return ErrorResponse.error(HttpStatus.BAD_REQUEST, "올바른 날짜를 입력해주세요. (형식: yyyy-MM-dd)");
		}
		return ErrorResponse.error(HttpStatus.BAD_REQUEST, "요청 파라미터 형식이 올바르지 않습니다.");
	}
}
