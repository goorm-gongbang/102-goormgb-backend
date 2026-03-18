package com.goormgb.be.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
	private final ErrorCode errorCode;

	public CustomException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public CustomException(ErrorCode errorCode, String detail, Throwable cause) {
		super(detail, cause);
		this.errorCode = errorCode;
	}
}
