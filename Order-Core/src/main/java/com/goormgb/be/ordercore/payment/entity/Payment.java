package com.goormgb.be.ordercore.payment.entity;

import java.time.Instant;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;
import com.goormgb.be.ordercore.payment.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
		name = "payments",
		indexes = {
				@Index(name = "idx_payments_status", columnList = "status")
		},
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_payments_order_id", columnNames = {"order_id"})
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	private Order order;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 30)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private PaymentStatus status;

	@Column(name = "paid_at")
	private Instant paidAt;

	// 무통장 입금 전용 필드 (BANK_TRANSFER 선택 시에만 값 존재)
	@Column(name = "account_bank", length = 50)
	private String accountBank;

	@Column(name = "account_number", length = 50)
	private String accountNumber;

	@Column(name = "account_holder", length = 50)
	private String accountHolder;

	@Column(name = "deposit_deadline")
	private Instant depositDeadline;

	@Builder
	public Payment(
			Order order,
			PaymentMethod paymentMethod,
			String accountBank,
			String accountNumber,
			String accountHolder,
			Instant depositDeadline
	) {
		this.order = order;
		this.paymentMethod = paymentMethod;
		this.status = PaymentStatus.PENDING;
		this.accountBank = accountBank;
		this.accountNumber = accountNumber;
		this.accountHolder = accountHolder;
		this.depositDeadline = depositDeadline;
	}

	public void complete() {
		this.status = PaymentStatus.COMPLETED;
		this.paidAt = Instant.now();
	}

	public void cancel() {
		this.status = PaymentStatus.CANCELLED;
	}

	public void refund() {
		this.status = PaymentStatus.REFUNDED;
	}
}
