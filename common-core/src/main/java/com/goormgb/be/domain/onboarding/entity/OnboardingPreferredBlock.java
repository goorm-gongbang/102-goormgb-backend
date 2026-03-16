package com.goormgb.be.domain.onboarding.entity;

import com.goormgb.be.global.entity.BaseEntity;
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
	name = "onboarding_preferred_blocks",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"user_id", "block_id"})
	},
	indexes = {
		@Index(name = "idx_onboarding_preferred_blocks_user_id", columnList = "user_id"),
		@Index(name = "idx_onboarding_preferred_blocks_block_id", columnList = "block_id")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingPreferredBlock extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "block_id", nullable = false)
	private Long blockId;

	@Builder
	public OnboardingPreferredBlock(User user, Long blockId) {
		this.user = user;
		this.blockId = blockId;
	}
}