package com.goormgb.be.onboarding.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.onboarding.enums.*;
import com.goormgb.be.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "onboarding_preferences", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"user_id", "priority"}),
	@UniqueConstraint(columnNames = {"user_id", "viewpoint"}),
	@UniqueConstraint(columnNames = {"user_id", "seat_height"}),
	@UniqueConstraint(columnNames = {"user_id", "section"})
}, indexes = {
	@Index(name = "idx_onboarding_preferences_user_id", columnList = "user_id"),
	@Index(name = "idx_onboarding_preferences_price_mode", columnList = "price_mode"),
	@Index(name = "idx_onboarding_preferences_price_min", columnList = "price_min"),
	@Index(name = "idx_onboarding_preferences_price_max", columnList = "price_max")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingPreference extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "priority", nullable = false)
	private Integer priority;

	// 필수 항목
	@Enumerated(EnumType.STRING)
	@Column(name = "viewpoint", nullable = false, length = 30)
	private Viewpoint viewpoint;

	@Enumerated(EnumType.STRING)
	@Column(name = "seat_height", nullable = false, length = 20)
	private SeatHeight seatHeight;

	@Enumerated(EnumType.STRING)
	@Column(name = "section", nullable = false, length = 20)
	private Section section;

	// 선택 항목 (기본값 ANY)
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

	// 가격
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
			Integer priority,
			Viewpoint viewpoint,
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
		this.priority = priority;
		update(
				viewpoint,
				seatHeight,
				section,
				seatPositionPref,
				environmentPref,
				moodPref,
				obstructionSensitivity,
				priceMode,
				priceMin,
				priceMax
		);
	}

	public void update(
			Viewpoint viewpoint,
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
		this.viewpoint = viewpoint;
		this.seatHeight = seatHeight;
		this.section = section;
		this.seatPositionPref = seatPositionPref != null ? seatPositionPref : SeatPositionPref.ANY;
		this.environmentPref = environmentPref != null ? environmentPref : EnvironmentPref.ANY;
		this.moodPref = moodPref != null ? moodPref : MoodPref.ANY;
		this.obstructionSensitivity =
				obstructionSensitivity != null ? obstructionSensitivity : ObstructionSensitivity.NORMAL;
		this.priceMode = priceMode != null ? priceMode : PriceMode.ANY;
		this.priceMin = priceMin;
		this.priceMax = priceMax;
	}
}
