package com.goormgb.be.seat.section.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SectionCode {
	PREMIUM("테라존(중앙 프리미엄석)"),
	PURPLE("퍼플석(테이블석)"),
	EXCITING("익사이팅존"),
	BLUE("블루석"),
	ORANGE("오렌지석"),
	RED("레드석"),
	NAVY("네이비석"),
	GREEN("그린석(외야석)");

	private final String description;
}
