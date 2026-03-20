package com.goormgb.be.seat.metrics.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FallbackType {
	MAP_SEARCH("map_search"),
	RANDOM_ASSIGNMENT("random_assignment"),
	MANUAL_SELECTION("manual_selection");

	private final String value;
}