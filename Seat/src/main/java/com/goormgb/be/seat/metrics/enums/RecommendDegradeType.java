package com.goormgb.be.seat.metrics.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecommendDegradeType {
	NO_PREFERRED_BLOCK("no_preferred_block"),
	INSUFFICIENT_CONTIGUOUS_SEATS("insufficient_contiguous_seats"),
	LOWER_PRIORITY_BLOCK("lower_priority_block");

	private final String value;
}