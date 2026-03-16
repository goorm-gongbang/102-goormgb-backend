package com.goormgb.be.ordercore.qrtoken.entity;

import java.time.Instant;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "qr_tokens",
	indexes = {
		@Index(name = "idx_qr_tokens_order_id", columnList = "order_id"),
		@Index(name = "idx_qr_tokens_user_id", columnList = "user_id"),
		@Index(name = "idx_qr_tokens_expires_at", columnList = "expires_at")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_qr_tokens_qr_token", columnNames = {"qr_token"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QrToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "qr_token", nullable = false, unique = true, length = 512)
	private String qrToken;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Builder
	public QrToken(Order order, User user, String qrToken, Instant expiresAt) {
		this.order = order;
		this.user = user;
		this.qrToken = qrToken;
		this.expiresAt = expiresAt;
	}

	public boolean isExpired() {
		return Instant.now().isAfter(this.expiresAt);
	}
}
