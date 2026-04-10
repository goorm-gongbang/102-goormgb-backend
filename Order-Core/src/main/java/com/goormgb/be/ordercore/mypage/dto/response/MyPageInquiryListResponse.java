package com.goormgb.be.ordercore.mypage.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.ordercore.inquiry.entity.Inquiry;

public record MyPageInquiryListResponse(
	List<InquiryItem> inquiries
) {
	public record InquiryItem(
		Long inquiryId,
		String category,
		String title,
		String status,
		Instant createdAt
	) {
		public static InquiryItem of(Inquiry inquiry) {
			return new InquiryItem(
				inquiry.getId(),
				inquiry.getCategory().name(),
				inquiry.getTitle(),
				inquiry.getStatus().name(),
				inquiry.getCreatedAt()
			);
		}
	}

	public static MyPageInquiryListResponse of(List<Inquiry> inquiries) {
		return new MyPageInquiryListResponse(
			inquiries.stream()
				.map(InquiryItem::of)
				.toList()
		);
	}
}
