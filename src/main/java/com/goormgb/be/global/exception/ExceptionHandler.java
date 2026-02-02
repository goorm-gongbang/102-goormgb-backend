package com.goormgb.be.global.exception;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.goormgb.be.global.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse.ErrorData> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return ErrorResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다. 백엔드팀에 문의하세요.");
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse.ErrorData> handleCustomException(CustomException e) {
        return ErrorResponse.error(e.getErrorCode().getStatus(), e.getErrorCode().getMessage());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse.ErrorData> handleValidationException(MethodArgumentNotValidException e) {
        var details = Arrays.toString(e.getDetailMessageArguments());
        var message = details.split(",", 2)[1].replace("]", "").trim();
        return ErrorResponse.error(HttpStatus.BAD_REQUEST, message);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse.ErrorData> handleAuthorizationDenied(AuthorizationDeniedException e) {
        return ErrorResponse.error(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }
}
