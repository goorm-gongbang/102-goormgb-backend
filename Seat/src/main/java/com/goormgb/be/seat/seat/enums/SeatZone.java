package com.goormgb.be.seat.seat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatZone {
	LOW("하단"),
	MID("중단"),
	HIGH("상단");

	private final String description;
}