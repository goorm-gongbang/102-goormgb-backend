package com.goormgb.be.ordercore.inquiry.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.inquiry.enums.InquiryCategory;
import com.goormgb.be.ordercore.inquiry.enums.InquiryStatus;
import com.goormgb.be.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "inquiries",
	indexes = {
		@Index(name = "idx_inquiries_user_id", columnList = "user_id"),
		@Index(name = "idx_inquiries_user_id_created_at", columnList = "user_id, created_at"),
		@Index(name = "idx_inquiries_status", columnList = "status")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 30)
	private InquiryCategory category;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Lob
	@Column(name = "content", nullable = false)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private InquiryStatus status;

	@Column(name = "phone_number", length = 20)
	private String phoneNumber;

	@Builder
	public Inquiry(
		User user,
		InquiryCategory category,
		String title,
		String content,
		String phoneNumber
	) {
		this.user = user;
		this.category = category;
		this.title = title;
		this.content = content;
		this.status = InquiryStatus.REGISTERED;
		this.phoneNumber = phoneNumber;
	}

	public void updateStatus(InquiryStatus status) {
		this.status = status;
	}
}
