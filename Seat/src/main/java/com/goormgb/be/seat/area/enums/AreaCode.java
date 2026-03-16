package com.goormgb.be.seat.area.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AreaCode {
	HOME("1루(홈)"),
	AWAY("3루(어웨이)"),
	OUTFIELD("외야"),
	CENTER("중앙");

	private final String description;
}
