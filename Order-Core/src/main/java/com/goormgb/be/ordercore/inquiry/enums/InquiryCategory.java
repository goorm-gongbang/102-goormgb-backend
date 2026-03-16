package com.goormgb.be.ordercore.inquiry.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryCategory {

	BOOKING("예매/상품"),
	PAYMENT("결제/수수료"),
	DELIVERY("배송/반송"),
	SYSTEM_ERROR("시스템 오류"),
	ETC("기타");

	private final String description;
}
