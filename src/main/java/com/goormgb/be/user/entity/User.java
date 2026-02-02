package com.goormgb.be.user.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.user.enums.UserStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private UserStatus status = UserStatus.ACTIVATE;

	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "nickname", length = 100)
	private String nickname;

	@Column(name = "onboarding_completed", nullable = false)
	private Boolean onboardingCompleted = false;

	@Column(name = "onboarding_completed_at")
	private OffsetDateTime onboardingCompletedAt;

	@Column(name = "last_login_at")
	private OffsetDateTime lastLoginAt;

	@Column(name = "marketing_consent", nullable = false)
	private Boolean marketingConsent = false;

	@Column(name = "marketing_consented_at")
	private OffsetDateTime marketingConsentedAt;

	@Builder
	public User(String email, String nickname) {
		this.email = email;
		this.nickname = nickname;
		this.status = UserStatus.ACTIVATE;
		this.onboardingCompleted = false;
		this.marketingConsent = false;
	}

	public void completeOnboarding() {
		this.onboardingCompleted = true;
		this.onboardingCompletedAt = OffsetDateTime.now();
	}

	public void updateLastLoginAt() {
		this.lastLoginAt = OffsetDateTime.now();
	}

	public void updateMarketingConsent(boolean consent) {
		this.marketingConsent = consent;
		if (consent) {
			this.marketingConsentedAt = OffsetDateTime.now();
		}
	}

	public void deactivate() {
		this.status = UserStatus.DEACTIVATE;
	}

	public void activate() {
		this.status = UserStatus.ACTIVATE;
	}
}