package com.goormgb.be.ordercore.mypage.dto.response;

public record MyPageInquiryCreateResponse(
	Long inquiryId
) {
	public static MyPageInquiryCreateResponse of(Long inquiryId) {
		return new MyPageInquiryCreateResponse(inquiryId);
	}
}
