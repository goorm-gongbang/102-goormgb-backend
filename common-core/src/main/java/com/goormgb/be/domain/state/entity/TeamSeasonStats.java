package com.goormgb.be.domain.state.entity;

import java.math.BigDecimal;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "team_season_stats", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"club_id", "season_year"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamSeasonStats extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "club_id", nullable = false)
	private Club club;

	@Column(name = "season_year", nullable = false)
	private int seasonYear;

	@Column(name = "season_ranking")
	private Integer seasonRanking;

	@Column(name = "wins")
	private Integer wins;

	@Column(name = "draws")
	private Integer draws;

	@Column(name = "losses")
	private Integer losses;

	@Column(name = "win_rate", precision = 5, scale = 3)
	private BigDecimal winRate;

	@Column(name = "batting_average", precision = 5, scale = 3)
	private BigDecimal battingAverage;

	@Column(name = "era", precision = 4, scale = 2)
	private BigDecimal era;

	@Column(name = "games_behind", precision = 4, scale = 1)
	private BigDecimal gamesBehind;

	@Builder
	public TeamSeasonStats(Club club, int seasonYear, int seasonRanking, int wins, int draws, int losses,
		BigDecimal winRate, BigDecimal battingAverage, BigDecimal era, BigDecimal gamesBehind) {
		this.club = club;
		this.seasonYear = seasonYear;
		this.seasonRanking = seasonRanking;
		this.wins = wins;
		this.draws = draws;
		this.losses = losses;
		this.winRate = winRate;
		this.battingAverage = battingAverage;
		this.era = era;
		this.gamesBehind = gamesBehind;
	}
}
