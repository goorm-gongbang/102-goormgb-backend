package com.goormgb.be.ordercore.inquiry.entity;

import com.goormgb.be.global.encryption.EncryptionConverter;
import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.inquiry.enums.InquiryCategory;
import com.goormgb.be.ordercore.inquiry.enums.InquiryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
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

	@Column(name = "user_id", nullable = false)
	private Long userId;

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

	@Convert(converter = EncryptionConverter.class)
	@Column(name = "phone_number", length = 512)
	private String phoneNumber;

	@Column(name = "file_key", length = 500)
	private String fileKey;

	@Builder
	public Inquiry(
		Long userId,
		InquiryCategory category,
		String title,
		String content,
		String phoneNumber
	) {
		this.userId = userId;
		this.category = category;
		this.title = title;
		this.content = content;
		this.status = InquiryStatus.REGISTERED;
		this.phoneNumber = phoneNumber;
	}

	public static Inquiry create(
		Long userId,
		InquiryCategory category,
		String title,
		String content,
		String phoneNumber
	) {
		return Inquiry.builder()
			.userId(userId)
			.category(category)
			.title(title)
			.content(content)
			.phoneNumber(phoneNumber)
			.build();
	}

	public void updateStatus(InquiryStatus status) {
		this.status = status;
	}

	public void updateFileKey(String fileKey) {
		this.fileKey = fileKey;
	}
}
