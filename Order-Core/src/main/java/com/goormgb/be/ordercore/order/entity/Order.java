package com.goormgb.be.ordercore.order.entity;

import java.time.Instant;
import java.util.UUID;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.global.encryption.EncryptionConverter;
import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "orders",
	indexes = {
		@Index(name = "idx_orders_user_id", columnList = "user_id"),
		@Index(name = "idx_orders_match_id", columnList = "match_id"),
		@Index(name = "idx_orders_user_id_status", columnList = "user_id, status"),
		@Index(name = "idx_orders_user_id_status_match", columnList = "user_id, status, match_id"),
		@Index(name = "idx_orders_user_id_created_at", columnList = "user_id, created_at"),
		@Index(name = "idx_orders_status", columnList = "status"),
		@Index(name = "idx_orders_order_number", columnList = "order_number")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

	private static final int DEFAULT_BOOKING_FEE = 2000;

	@Column(name = "order_number", nullable = false, unique = true, updatable = false, length = 40)
	private String orderNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", nullable = false)
	private Match match;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private OrderStatus status;

	@Column(name = "total_amount", nullable = false)
	private Integer totalAmount;

	@Column(name = "booking_fee", nullable = false)
	private Integer bookingFee;

	@Column(name = "cancellation_fee", nullable = false)
	private Integer cancellationFee;

	@Column(name = "refunded_amount")
	private Integer refundedAmount;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Convert(converter = EncryptionConverter.class)
	@Column(name = "orderer_name", nullable = false, length = 512)
	private String ordererName;

	@Convert(converter = EncryptionConverter.class)
	@Column(name = "orderer_email", nullable = false, length = 512)
	private String ordererEmail;

	@Convert(converter = EncryptionConverter.class)
	@Column(name = "orderer_phone", nullable = false, length = 512)
	private String ordererPhone;

	@Convert(converter = EncryptionConverter.class)
	@Column(name = "orderer_birth_date", nullable = false, length = 512)
	private String ordererBirthDate;

	@Builder
	public Order(
		User user,
		Match match,
		Integer totalAmount,
		String ordererName,
		String ordererEmail,
		String ordererPhone,
		String ordererBirthDate
	) {
		this.user = user;
		this.match = match;
		this.status = OrderStatus.PAYMENT_PENDING;
		this.totalAmount = totalAmount;
		this.bookingFee = DEFAULT_BOOKING_FEE;
		this.cancellationFee = 0;
		this.ordererName = ordererName;
		this.ordererEmail = ordererEmail;
		this.ordererPhone = ordererPhone;
		this.ordererBirthDate = ordererBirthDate;
	}

	@PrePersist
	private void generateOrderNumber() {
		if (this.orderNumber == null) {
			this.orderNumber = "ORD-" + UUID.randomUUID();
		}
	}

	public void updateStatus(OrderStatus status) {
		this.status = status;
	}

	public void cancel(Integer cancellationFee, Integer refundedAmount) {
		updateCancellationInfo(OrderStatus.CANCEL_REQUESTED, cancellationFee, refundedAmount, Instant.now());
	}

	/**
	 * 토스페이/카카오페이 결제 취소 — 즉시 CANCELLED 처리
	 */
	public void cancelComplete(Integer cancellationFee, Integer refundedAmount, Instant cancelledAt) {
		updateCancellationInfo(OrderStatus.CANCELLED, cancellationFee, refundedAmount, cancelledAt);
	}

	/**
	 * 무통장 입금 환불 완료 — 즉시 REFUND_COMPLETED 처리
	 */
	public void refundComplete(Integer cancellationFee, Integer refundedAmount, Instant cancelledAt) {
		updateCancellationInfo(OrderStatus.REFUND_COMPLETED, cancellationFee, refundedAmount, cancelledAt);
	}

	private void updateCancellationInfo(OrderStatus status, Integer cancellationFee, Integer refundedAmount,
		Instant cancelledAt) {
		this.status = status;
		this.cancellationFee = cancellationFee;
		this.refundedAmount = refundedAmount;
		this.cancelledAt = cancelledAt;
	}
}
