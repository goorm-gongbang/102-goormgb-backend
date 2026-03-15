package com.goormgb.be.ordercore.cancellation.entity;

import java.math.BigDecimal;

import com.goormgb.be.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 취소 수수료 정책 (시드 데이터로 고정 운영, 런타임 변경 금지)
 *
 * 시드 데이터:
 *   days_min=0,  days_max=0,    cancellable=false, fee_rate=0.000, booking_fee_refundable=false  → 경기 당일: 취소 불가
 *   days_min=1,  days_max=6,    cancellable=true,  fee_rate=0.100, booking_fee_refundable=false  → D-1~D-6: 10% + 예매 대행 수수료 환불 불가
 *   days_min=7,  days_max=null, cancellable=true,  fee_rate=0.000, booking_fee_refundable=true   → D-7+: 무료, 예매 당일 취소 시에만 예매 대행 수수료 환불
 */
@Entity
@Table(name = "cancellation_fee_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CancellationFeePolicy extends BaseEntity {

	@Column(name = "days_before_match_min", nullable = false)
	private Integer daysBeforeMatchMin;

	/**
	 * null이면 상한 없음 (D-N 이상 모두 적용)
	 */
	@Column(name = "days_before_match_max")
	private Integer daysBeforeMatchMax;

	@Column(name = "cancellable", nullable = false)
	private Boolean cancellable;

	@Column(name = "ticket_fee_rate", nullable = false, precision = 5, scale = 3)
	private BigDecimal ticketFeeRate;

	/**
	 * true이더라도 예매 당일 취소 시에만 실제 환불 (애플리케이션 레벨 검증 필요)
	 */
	@Column(name = "booking_fee_refundable", nullable = false)
	private Boolean bookingFeeRefundable;

	@Builder
	public CancellationFeePolicy(
		Integer daysBeforeMatchMin,
		Integer daysBeforeMatchMax,
		Boolean cancellable,
		BigDecimal ticketFeeRate,
		Boolean bookingFeeRefundable
	) {
		this.daysBeforeMatchMin = daysBeforeMatchMin;
		this.daysBeforeMatchMax = daysBeforeMatchMax;
		this.cancellable = cancellable;
		this.ticketFeeRate = ticketFeeRate;
		this.bookingFeeRefundable = bookingFeeRefundable;
	}
}
