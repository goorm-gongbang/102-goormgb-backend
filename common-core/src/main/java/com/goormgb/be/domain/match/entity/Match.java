package com.goormgb.be.domain.match.entity;

import java.time.Instant;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.stadium.entity.Stadium;
import com.goormgb.be.global.entity.BaseEntity;

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
@Table(name = "matches", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"stadium_id", "match_at"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match extends BaseEntity {

	@Column(name = "match_at", nullable = false)
	private Instant matchAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "home_club_id", nullable = false)
	private Club homeClub;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "away_club_id", nullable = false)
	private Club awayClub;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stadium_id", nullable = false)
	private Stadium stadium;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_status", nullable = false, length = 20)
	private SaleStatus saleStatus;

	@Builder
	public Match(Instant matchAt, Club homeClub, Club awayClub, Stadium stadium, SaleStatus saleStatus) {
		this.matchAt = matchAt;
		this.homeClub = homeClub;
		this.awayClub = awayClub;
		this.stadium = stadium;
		this.saleStatus = saleStatus;
	}

	public static Match create(Instant matchAt, Club homeClub, Club awayClub, Stadium stadium,
		SaleStatus saleStatus) {
		return Match.builder()
			.matchAt(matchAt)
			.homeClub(homeClub)
			.awayClub(awayClub)
			.stadium(stadium)
			.saleStatus(saleStatus)
			.build();
	}

	public void updateSaleStatus(SaleStatus newStatus) {
		this.saleStatus = newStatus;
	}
}
