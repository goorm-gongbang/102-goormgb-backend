package com.goormgb.be.seat.block.entity;

import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.seat.section.entity.Section;

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
	name = "blocks",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_block_section_code",
			columnNames = {"section_id", "block_code"}
		)
	},
	indexes = {
		@Index(name = "idx_block_section_id", columnList = "section_id"),
		@Index(name = "idx_block_viewpoint", columnList = "viewpoint"),
		@Index(name = "idx_block_home_cheer_rank", columnList = "home_cheer_rank"),
		@Index(name = "idx_block_away_cheer_rank", columnList = "away_cheer_rank")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Block extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "section_id", nullable = false)
	private Section section;

	@Column(name = "block_code", nullable = false, length = 20)
	private String blockCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "viewpoint", nullable = false, length = 30)
	private Viewpoint viewpoint;

	@Column(name = "home_cheer_rank")
	private Integer homeCheerRank;

	@Column(name = "away_cheer_rank")
	private Integer awayCheerRank;

	@Builder
	public Block(
		Section section,
		String blockCode,
		Viewpoint viewpoint,
		Integer homeCheerRank,
		Integer awayCheerRank
	) {
		this.section = section;
		this.blockCode = blockCode;
		this.viewpoint = viewpoint;
		this.homeCheerRank = homeCheerRank;
		this.awayCheerRank = awayCheerRank;
	}
}