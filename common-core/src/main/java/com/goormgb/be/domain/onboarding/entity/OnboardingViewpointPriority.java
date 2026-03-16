package com.goormgb.be.domain.onboarding.entity;

import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	name = "onboarding_viewpoint_priorities",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_viewpoint_priority_user_id_priority",
			columnNames = {"user_id", "priority"}
		),
		@UniqueConstraint(
			name = "uk_viewpoint_priority_user_id_viewpoint",
			columnNames = {"user_id", "viewpoint"}
		)
	},
	indexes = {
		@Index(name = "idx_viewpoint_priority_user_id", columnList = "user_id")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingViewpointPriority extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "priority", nullable = false)
	private Integer priority;

	@Enumerated(EnumType.STRING)
	@Column(name = "viewpoint", nullable = false, length = 30)
	private Viewpoint viewpoint;

	@Builder
	public OnboardingViewpointPriority(User user, Integer priority, Viewpoint viewpoint) {
		this.user = user;
		this.priority = priority;
		this.viewpoint = viewpoint;
	}
}
