package com.goormgb.be.ordercore.order.entity;

import java.time.Instant;

import com.goormgb.be.global.encryption.EncryptionConverter;
import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.order.enums.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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
		@Index(name = "idx_orders_status", columnList = "status")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

	private static final int DEFAULT_BOOKING_FEE = 2000;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "match_id", nullable = false)
	private Long matchId;

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

	// --- 비정규화 컬럼 (주문 생성 시점 스냅샷) ---

	@Column(name = "user_nickname")
	private String userNickname;

	@Column(name = "match_title", length = 200)
	private String matchTitle;

	@Column(name = "match_date")
	private Instant matchDate;

	@Column(name = "stadium_name", length = 100)
	private String stadiumName;

	@Column(name = "home_club_name", length = 50)
	private String homeClubName;

	@Column(name = "away_club_name", length = 50)
	private String awayClubName;

	@Builder
	public Order(
		Long userId,
		Long matchId,
		Integer totalAmount,
		String ordererName,
		String ordererEmail,
		String ordererPhone,
		String ordererBirthDate,
		String userNickname,
		String matchTitle,
		Instant matchDate,
		String stadiumName,
		String homeClubName,
		String awayClubName
	) {
		this.userId = userId;
		this.matchId = matchId;
		this.status = OrderStatus.PAYMENT_PENDING;
		this.totalAmount = totalAmount;
		this.bookingFee = DEFAULT_BOOKING_FEE;
		this.cancellationFee = 0;
		this.ordererName = ordererName;
		this.ordererEmail = ordererEmail;
		this.ordererPhone = ordererPhone;
		this.ordererBirthDate = ordererBirthDate;
		this.userNickname = userNickname;
		this.matchTitle = matchTitle;
		this.matchDate = matchDate;
		this.stadiumName = stadiumName;
		this.homeClubName = homeClubName;
		this.awayClubName = awayClubName;
	}

	public void updateStatus(OrderStatus status) {
		this.status = status;
	}

	public void cancel(Integer cancellationFee, Integer refundedAmount) {
		updateCancellationInfo(OrderStatus.CANCEL_REQUESTED, cancellationFee, refundedAmount, Instant.now());
	}

	/**
	 * 토스페이/카카오페이 결제 취소 -- 즉시 CANCELLED 처리
	 */
	public void cancelComplete(Integer cancellationFee, Integer refundedAmount, Instant cancelledAt) {
		updateCancellationInfo(OrderStatus.CANCELLED, cancellationFee, refundedAmount, cancelledAt);
	}

	/**
	 * 무통장 입금 환불 완료 -- 즉시 REFUND_COMPLETED 처리
	 */
	public void refundComplete(Integer cancellationFee, Integer refundedAmount, Instant cancelledAt) {
		updateCancellationInfo(OrderStatus.REFUND_COMPLETED, cancellationFee, refundedAmount, cancelledAt);
	}

	public void updateUserNickname(String userNickname) {
		this.userNickname = userNickname;
	}

	private void updateCancellationInfo(OrderStatus status, Integer cancellationFee, Integer refundedAmount,
		Instant cancelledAt) {
		this.status = status;
		this.cancellationFee = cancellationFee;
		this.refundedAmount = refundedAmount;
		this.cancelledAt = cancelledAt;
	}
}
