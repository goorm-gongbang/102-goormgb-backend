package com.goormgb.be.user.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.goormgb.be.user.enums.WithdrawStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "withdrawal_requests", indexes = {
		@Index(name = "idx_withdrawal_requests_effective_at", columnList = "effective_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalRequest {

	private static final int GRACE_PERIOD_DAYS = 30;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "requested_at", nullable = false)
	private Instant requestedAt;

	@Column(name = "effective_at", nullable = false)
	private Instant effectiveAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private WithdrawStatus status = WithdrawStatus.REQUESTED;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = Instant.now();
	}

	@Builder
	public WithdrawalRequest(User user) {
		this.user = user;
		this.requestedAt = Instant.now();
		this.effectiveAt = this.requestedAt.plus(GRACE_PERIOD_DAYS, ChronoUnit.DAYS);
		this.status = WithdrawStatus.REQUESTED;
	}

	public boolean isExpired() {
		return Instant.now().isAfter(this.effectiveAt);
	}
}
