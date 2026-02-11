package com.goormgb.be.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
	ACTIVATE("활성"),
	DEACTIVATE("비활성");

	private final String description;
}
