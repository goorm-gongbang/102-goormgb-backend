package com.goormgb.be.seat.section.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.seat.area.entity.Area;
import com.goormgb.be.seat.section.enums.SectionCode;

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
	name = "sections",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_section_area_code",
			columnNames = {"area_id", "code"}
		)
	},
	indexes = {
		@Index(name = "idx_section_area_id", columnList = "area_id")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "area_id", nullable = false)
	private Area area;

	@Enumerated(EnumType.STRING)
	@Column(name = "code", nullable = false, length = 30)
	private SectionCode code;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "color_hex", length = 10)
	private String colorHex;

	@Builder
	public Section(
		Area area,
		SectionCode code,
		String name,
		String colorHex
	) {
		this.area = area;
		this.code = code;
		this.name = name;
		this.colorHex = colorHex;
	}
}
