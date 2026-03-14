package com.goormgb.be.ordercore.inquiry.entity;

import java.time.Instant;

import com.goormgb.be.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "inquiry_answers",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_inquiry_answers_inquiry_id", columnNames = {"inquiry_id"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryAnswer extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inquiry_id", nullable = false, unique = true)
	private Inquiry inquiry;

	@Lob
	@Column(name = "content", nullable = false)
	private String content;

	@Column(name = "answered_at", nullable = false)
	private Instant answeredAt;

	@Builder
	public InquiryAnswer(Inquiry inquiry, String content) {
		this.inquiry = inquiry;
		this.content = content;
		this.answeredAt = Instant.now();
	}
}
