package com.goormgb.be.seat.matchSeat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchSeatSaleStatus {

	AVAILABLE("예매 가능"),
	SOLD("판매 완료"),
	BLOCKED("판매 차단");

	private final String description;
}