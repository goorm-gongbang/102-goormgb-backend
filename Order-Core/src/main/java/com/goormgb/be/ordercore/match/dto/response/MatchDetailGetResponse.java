package com.goormgb.be.ordercore.match.dto.response;

import java.time.Instant;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.ordercore.match.dto.MatchGuideDto;

public record MatchDetailGetResponse(
	Long matchId,
	Instant matchAt,
	SaleStatus saleStatus,
	ClubDto homeClub,
	ClubDto awayClub,
	MatchGuideDto matchGuide
) {
	public static MatchDetailGetResponse of(Match match, MatchGuideDto matchGuide) {
		return new MatchDetailGetResponse(
			match.getId(),
			match.getMatchAt(),
			match.getSaleStatus(),
			ClubDto.from(match.getHomeClub()),
			ClubDto.from(match.getAwayClub()),
			matchGuide
		);
	}

	public record ClubDto(
		Long clubId,
		String koName,
		String enName,
		String logoImg,
		String clubColor
	) {
		public static ClubDto from(Club club) {
			return new ClubDto(
				club.getId(),
				club.getKoName(),
				club.getEnName(),
				club.getLogoImg(),
				club.getClubColor()
			);
		}
	}
}
