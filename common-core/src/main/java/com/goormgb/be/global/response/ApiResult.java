package com.goormgb.be.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResult<T> {
	private String code;
	private String message;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private T data;

	public static ApiResult<Void> ok() {
		return of("OK", "성공", null);
	}

	public static ApiResult<Void> ok(String message) {
		return of("OK", message, null);
	}

	public static <T> ApiResult<T> ok(T data) {
		return of("OK", "성공", data);
	}

	public static <T> ApiResult<T> ok(String message, T data) {
		return of("OK", message, data);
	}

	private static <T> ApiResult<T> of(String code, String message, T data) {
		return new ApiResult<>(code, message, data);
	}
}
