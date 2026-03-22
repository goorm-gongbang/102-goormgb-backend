package com.goormgb.be.seat.metrics.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatHoldFailReason {
	CONFLICT("conflict"),
	VALIDATION("validation"),
	TIMEOUT("timeout"),
	SYSTEM_ERROR("system_error");

	private final String value;
}