package com.goormgb.be.seat.pricePolicy.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DayType {

	WEEKDAY("평일"),
	WEEKEND("주말");

	private final String description;
}