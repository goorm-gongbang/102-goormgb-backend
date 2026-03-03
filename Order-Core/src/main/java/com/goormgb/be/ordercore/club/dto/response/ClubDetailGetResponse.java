package com.goormgb.be.ordercore.club.dto.response;

import java.math.BigDecimal;

import com.goormgb.be.ordercore.club.entity.Club;
import com.goormgb.be.ordercore.stadium.entity.Stadium;
import com.goormgb.be.ordercore.state.entity.TeamSeasonStats;

public record ClubDetailGetResponse(
		Long clubId,
		String koName,
		String logoImg,
		String clubColor,
		StadiumDto stadium,
		String homepageRedirectUrl,
		CurrentSeasonStatsDto currentSeasonStats
) {
	public static ClubDetailGetResponse of(Club club, TeamSeasonStats stats) {
		return new ClubDetailGetResponse(
				club.getId(),
				club.getKoName(),
				club.getLogoImg(),
				club.getClubColor(),
				StadiumDto.from(club.getStadium()),
				club.getHomepageRedirectUrl(),
				stats == null ? null : CurrentSeasonStatsDto.from(stats)
		);
	}

	public record StadiumDto(Long stadiumId, String koName) {
		public static StadiumDto from(Stadium stadium) {
			return new StadiumDto(stadium.getId(), stadium.getKoName());
		}
	}

	public record CurrentSeasonStatsDto(
			int seasonYear,
			Integer seasonRanking,
			Integer wins,
			Integer draws,
			Integer losses,
			BigDecimal winRate,
			BigDecimal battingAverage,
			BigDecimal era,
			BigDecimal gamesBehind
	) {
		public static CurrentSeasonStatsDto from(TeamSeasonStats stats) {
			return new CurrentSeasonStatsDto(
					stats.getSeasonYear(),
					stats.getSeasonRanking(),
					stats.getWins(),
					stats.getDraws(),
					stats.getLosses(),
					stats.getWinRate(),
					stats.getBattingAverage(),
					stats.getEra(),
					stats.getGamesBehind()
			);
		}
	}
}
