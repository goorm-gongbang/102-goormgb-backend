package com.goormgb.be.seat.metrics.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatHoldMode {
	RECOMMEND("recommend"),
	MAP("map");

	private final String value;
}