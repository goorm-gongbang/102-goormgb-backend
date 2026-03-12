package com.goormgb.be.seat.seatHold.entity;

import java.time.Instant;

import com.goormgb.be.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "seat_holds",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_seat_holds_match_seat_id",
			columnNames = {"match_seat_id"}
		)
	},
	indexes = {
		@Index(
			name = "idx_seat_holds_match_id",
			columnList = "match_id"
		),
		@Index(
			name = "idx_seat_holds_user_id",
			columnList = "user_id"
		),
		@Index(
			name = "idx_seat_holds_expires_at",
			columnList = "expires_at"
		)
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatHold extends BaseEntity {

	@Column(name = "match_seat_id", nullable = false)
	private Long matchSeatId;

	@Column(name = "match_id", nullable = false)
	private Long matchId;

	@Column(name = "seat_id", nullable = false)
	private Long seatId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Builder
	public SeatHold(
		Long matchSeatId,
		Long matchId,
		Long seatId,
		Long userId,
		Instant expiresAt
	) {
		this.matchSeatId = matchSeatId;
		this.matchId = matchId;
		this.seatId = seatId;
		this.userId = userId;
		this.expiresAt = expiresAt;
	}

	public boolean isExpired(Instant now) {
		return this.expiresAt.isBefore(now);
	}

	public boolean isOwnedBy(Long userId) {
		return this.userId.equals(userId);
	}

	public void extendHold(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}
}