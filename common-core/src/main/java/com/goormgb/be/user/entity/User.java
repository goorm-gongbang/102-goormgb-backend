package com.goormgb.be.user.entity;

import java.time.Instant;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.user.enums.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Column(name = "onboarding_completed", nullable = false)
	private Boolean onboardingCompleted = false;

	@Column(name = "onboarding_completed_at")
	private Instant onboardingCompletedAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "marketing_consent", nullable = false)
	private Boolean marketingConsent = false;

	@Column(name = "marketing_consented_at")
	private Instant marketingConsentedAt;

	@Builder
	public User(String email, String nickname, String profileImageUrl) {
		this.email = email;
		this.nickname = nickname;
		this.profileImageUrl = profileImageUrl;
		this.onboardingCompleted = false;
		this.marketingConsent = false;
	}

	public boolean isCompletedOnboarding() {
		return Boolean.TRUE.equals(this.onboardingCompleted);
	}

	public void completeOnboarding() {
		this.onboardingCompleted = true;
		this.onboardingCompletedAt = Instant.now();
	}

	public void updateLastLoginAt() {
		this.lastLoginAt = Instant.now();
	}

	public void updateMarketingConsent(boolean consent) {
		this.marketingConsent = consent;
		if (consent) {
			this.marketingConsentedAt = Instant.now();
		}
	}

	public void deactivate() {
		this.status = UserStatus.DEACTIVATE;
	}

	public void activate() {
		this.status = UserStatus.ACTIVATE;
	}

	public static User createOAuthUser(String email, String nickname, String profileImageUrl) {
		return User.builder()
				.email(email)
				.nickname(nickname)
				.profileImageUrl(profileImageUrl)
				.build();
	}
}
