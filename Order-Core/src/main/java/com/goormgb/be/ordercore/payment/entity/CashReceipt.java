package com.goormgb.be.ordercore.payment.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.payment.enums.CashReceiptPurpose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "cash_receipts",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_cash_receipts_payment_id", columnNames = {"payment_id"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashReceipt extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id", nullable = false, unique = true)
	private Payment payment;

	@Enumerated(EnumType.STRING)
	@Column(name = "purpose", nullable = false, length = 30)
	private CashReceiptPurpose purpose;

	/**
	 * PERSONAL_DEDUCTION: 현금영수증용 전화번호
	 * BUSINESS_EXPENSE: 사업자번호
	 */
	@Column(name = "number", nullable = false, length = 50)
	private String number;

	@Builder
	public CashReceipt(Payment payment, CashReceiptPurpose purpose, String number) {
		this.payment = payment;
		this.purpose = purpose;
		this.number = number;
	}
}
