package com.goormgb.be.ordercore.inquiry.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryStatus {

	REGISTERED("접수 완료"),
	PENDING("처리 중"),
	ANSWERED("답변 완료"),
	CLOSED("종료");

	private final String description;
}
