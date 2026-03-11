package com.goormgb.be.seat.area.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AreaCode {
	HOME("1루(홈)"),
	AWAY("3루(어웨이)"),
	PREMIUM("프리미엄");

	private final String description;
}
