package com.goormgb.be.user.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.user.enums.UserStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
	private LocalDateTime onboardingCompletedAt;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@Column(name = "marketing_consent", nullable = false)
	private Boolean marketingConsent = false;

	@Column(name = "marketing_consented_at")
	private LocalDateTime marketingConsentedAt;

	@Builder
	public User(String email, String nickname) {
		this.email = email;
		this.nickname = nickname;
		this.onboardingCompleted = false;
		this.marketingConsent = false;
	}

	public void completeOnboarding() {
		this.onboardingCompleted = true;
		this.onboardingCompletedAt = LocalDateTime.now(ZoneOffset.UTC);
	}

	public void updateLastLoginAt() {
		this.lastLoginAt = LocalDateTime.now(ZoneOffset.UTC);
	}

	public void updateMarketingConsent(boolean consent) {
		this.marketingConsent = consent;
		if (consent) {
			this.marketingConsentedAt = LocalDateTime.now(ZoneOffset.UTC);
		}
	}

	public void deactivate() {
		this.status = UserStatus.DEACTIVATE;
	}

	public void activate() {
		this.status = UserStatus.ACTIVATE;
	}
}