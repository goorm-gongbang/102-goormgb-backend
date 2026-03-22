package com.goormgb.be.ordercore.metrics.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderDraftEntryPoint {
	RECOMMEND("recommend"),
	MAP("map");

	private final String value;
}