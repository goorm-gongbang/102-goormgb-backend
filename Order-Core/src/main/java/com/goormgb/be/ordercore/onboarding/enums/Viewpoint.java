package com.goormgb.be.ordercore.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Viewpoint {
	CENTER("중앙"),
	INFIELD_1B("1루 내야"),
	INFIELD_3B("3루 내야"),
	OUTFIELD_L("외야(좌)"),
	OUTFIELD_C("외야 (중)"),
	OUTFIELD_R("외야 (우)");

	private final String description;
}
