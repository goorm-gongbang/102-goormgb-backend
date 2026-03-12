package com.goormgb.be.domain.onboarding.entity;

import java.util.Objects;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.EnvironmentPref;
import com.goormgb.be.domain.onboarding.enums.MoodPref;
import com.goormgb.be.domain.onboarding.enums.ObstructionSensitivity;
import com.goormgb.be.domain.onboarding.enums.PriceMode;
import com.goormgb.be.domain.onboarding.enums.SeatHeight;
import com.goormgb.be.domain.onboarding.enums.SeatPositionPref;
import com.goormgb.be.domain.onboarding.enums.Section;
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
		name = "onboarding_preferences",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_onboarding_preferences_user_id",
						columnNames = {"user_id"}
				)
		},
		indexes = {
				@Index(name = "idx_onboarding_preferences_user_id", columnList = "user_id"),
				@Index(name = "idx_onboarding_preferences_favorite_club_id", columnList = "favorite_club_id")
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingPreference extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// ── 추천 알고리즘에 사용되는 필수 필드 ──

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "favorite_club_id", nullable = false)
	private Club favoriteClub;

	@Enumerated(EnumType.STRING)
	@Column(name = "cheer_proximity_pref", nullable = false, length = 20)
	private CheerProximityPref cheerProximityPref = CheerProximityPref.ANY;

	// ── 추천 미사용 - 옵셔널 필드 (입력 안 하면 기본값 ANY/NORMAL) ──

	@Enumerated(EnumType.STRING)
	@Column(name = "seat_height", nullable = false, length = 20)
	private SeatHeight seatHeight = SeatHeight.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "section", nullable = false, length = 20)
	private Section section = Section.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "seat_position_pref", nullable = false, length = 20)
	private SeatPositionPref seatPositionPref = SeatPositionPref.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "environment_pref", nullable = false, length = 20)
	private EnvironmentPref environmentPref = EnvironmentPref.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "mood_pref", nullable = false, length = 20)
	private MoodPref moodPref = MoodPref.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "obstruction_sensitivity", nullable = false, length = 30)
	private ObstructionSensitivity obstructionSensitivity = ObstructionSensitivity.NORMAL;

	@Enumerated(EnumType.STRING)
	@Column(name = "price_mode", nullable = false, length = 20)
	private PriceMode priceMode = PriceMode.ANY;

	@Column(name = "price_min")
	private Integer priceMin;

	@Column(name = "price_max")
	private Integer priceMax;

	@Builder
	public OnboardingPreference(
			User user,
			Club favoriteClub,
			CheerProximityPref cheerProximityPref,
			SeatHeight seatHeight,
			Section section,
			SeatPositionPref seatPositionPref,
			EnvironmentPref environmentPref,
			MoodPref moodPref,
			ObstructionSensitivity obstructionSensitivity,
			PriceMode priceMode,
			Integer priceMin,
			Integer priceMax
	) {
		this.user = user;
		this.favoriteClub = favoriteClub;
		applyOptionalFields(cheerProximityPref, seatHeight, section, seatPositionPref,
			environmentPref, moodPref, obstructionSensitivity, priceMode, priceMin, priceMax);
	}

	public void update(
			Club favoriteClub,
			CheerProximityPref cheerProximityPref,
			SeatHeight seatHeight,
			Section section,
			SeatPositionPref seatPositionPref,
			EnvironmentPref environmentPref,
			MoodPref moodPref,
			ObstructionSensitivity obstructionSensitivity,
			PriceMode priceMode,
			Integer priceMin,
			Integer priceMax
	) {
		this.favoriteClub = favoriteClub;
		applyOptionalFields(cheerProximityPref, seatHeight, section, seatPositionPref,
			environmentPref, moodPref, obstructionSensitivity, priceMode, priceMin, priceMax);
	}

	private void applyOptionalFields(
			CheerProximityPref cheerProximityPref,
			SeatHeight seatHeight,
			Section section,
			SeatPositionPref seatPositionPref,
			EnvironmentPref environmentPref,
			MoodPref moodPref,
			ObstructionSensitivity obstructionSensitivity,
			PriceMode priceMode,
			Integer priceMin,
			Integer priceMax
	) {
		this.cheerProximityPref = Objects.requireNonNullElse(cheerProximityPref, CheerProximityPref.ANY);
		this.seatHeight = Objects.requireNonNullElse(seatHeight, SeatHeight.ANY);
		this.section = Objects.requireNonNullElse(section, Section.ANY);
		this.seatPositionPref = Objects.requireNonNullElse(seatPositionPref, SeatPositionPref.ANY);
		this.environmentPref = Objects.requireNonNullElse(environmentPref, EnvironmentPref.ANY);
		this.moodPref = Objects.requireNonNullElse(moodPref, MoodPref.ANY);
		this.obstructionSensitivity = Objects.requireNonNullElse(obstructionSensitivity, ObstructionSensitivity.NORMAL);
		this.priceMode = Objects.requireNonNullElse(priceMode, PriceMode.ANY);
		this.priceMin = priceMin;
		this.priceMax = priceMax;
	}

	public void changeFavoriteClub(Club favoriteClub) {
		this.favoriteClub = favoriteClub;
	}
}
