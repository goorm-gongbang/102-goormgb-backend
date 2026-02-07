package com.goormgb.be.global.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.goormgb.be.global.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private HttpStatus status;
    private String message;

    public static ResponseEntity<ErrorData> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorData.of(status.series().name(), message));
    }

    public static ResponseEntity<ErrorData> error(ErrorCode errorCode) {
        return error(errorCode.getStatus(), errorCode.getMessage());
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorData {
        private String code;
        private String message;

        public static ErrorData of(String code, String message) {
            return new ErrorData(code, message);
        }
    }
}
