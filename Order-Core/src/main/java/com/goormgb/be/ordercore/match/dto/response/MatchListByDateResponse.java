package com.goormgb.be.ordercore.match.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.entity.Stadium;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;

public record MatchListByDateResponse(
	LocalDate date,
	int matchCount,
	List<MatchSummary> matches
) {
	public static MatchListByDateResponse of(LocalDate date, List<MatchSummary> matches) {
		return new MatchListByDateResponse(date, matches.size(), matches);
	}

	public record MatchSummary(
		Long matchId,
		Instant matchAt,
		SaleStatus saleStatus,
		Instant salesOpenAt,
		ClubDto homeClub,
		ClubDto awayClub,
		StadiumDto stadium
	) {
		public static MatchSummary of(Match m, Instant salesOpenAt) {
			return new MatchSummary(
				m.getId(),
				m.getMatchAt(),
				m.getSaleStatus(),
				salesOpenAt,
				ClubDto.from(m.getHomeClub()),
				ClubDto.from(m.getAwayClub()),
				StadiumDto.from(m.getStadium())
			);
		}
	}

	public record ClubDto(
		Long clubId,
		String koName,
		String enName,
		String logoImg
	) {
		public static ClubDto from(Club c) {
			return new ClubDto(c.getId(), c.getKoName(), c.getEnName(), c.getLogoImg());
		}
	}

	public record StadiumDto(
		Long stadiumId,
		String koName,
		String enName
	) {
		public static StadiumDto from(Stadium s) {
			return new StadiumDto(s.getId(), s.getKoName(), s.getEnName());
		}
	}
}