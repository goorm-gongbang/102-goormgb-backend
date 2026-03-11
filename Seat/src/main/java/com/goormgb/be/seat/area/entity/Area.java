package com.goormgb.be.seat.area.entity;

import com.goormgb.be.domain.stadium.entity.Stadium;
import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.seat.area.enums.AreaCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
	name = "areas",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_area_stadium_code", columnNames = {"stadium_id", "code"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Area extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stadium_id", nullable = false)
	private Stadium stadium;

	@Enumerated(EnumType.STRING)
	@Column(name = "code", nullable = false, length = 50)
	private AreaCode code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Builder
	public Area(
		Stadium stadium,
		AreaCode code,
		String name
	) {
		this.stadium = stadium;
		this.code = code;
		this.name = name;
	}
}
