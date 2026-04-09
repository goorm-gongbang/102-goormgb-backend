package com.goormgb.be.ordercore.mypage.dto.response;

import java.time.Instant;

import com.goormgb.be.ordercore.inquiry.entity.Inquiry;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "1:1 문의 상세 응답")
public record MyPageInquiryDetailResponse(
	Long inquiryId,
	String category,
	String title,
	String content,
	String phoneNumber,
	String status,
	boolean fileAttached,
	String downloadUrl,
	Instant createdAt
) {
	public static MyPageInquiryDetailResponse of(Inquiry inquiry, String downloadUrl) {
		return new MyPageInquiryDetailResponse(
			inquiry.getId(),
			inquiry.getCategory().name(),
			inquiry.getTitle(),
			inquiry.getContent(),
			inquiry.getPhoneNumber(),
			inquiry.getStatus().name(),
			inquiry.getFileKey() != null && downloadUrl != null,
			downloadUrl,
			inquiry.getCreatedAt()
		);
	}
}
