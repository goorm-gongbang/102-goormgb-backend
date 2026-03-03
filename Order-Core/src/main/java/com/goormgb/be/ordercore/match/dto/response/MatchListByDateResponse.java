package com.goormgb.be.ordercore.match.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.goormgb.be.ordercore.club.entity.Club;
import com.goormgb.be.ordercore.match.entity.Match;
import com.goormgb.be.ordercore.match.enums.SaleStatus;
import com.goormgb.be.ordercore.stadium.entity.Stadium;

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
			LocalDateTime matchAt,
			SaleStatus saleStatus,
			LocalDateTime salesOpenAt,
			ClubDto homeClub,
			ClubDto awayClub,
			StadiumDto stadium
	) {
		public static MatchSummary of(Match m, LocalDateTime salesOpenAt) {
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