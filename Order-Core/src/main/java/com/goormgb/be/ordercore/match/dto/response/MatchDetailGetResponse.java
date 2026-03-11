package com.goormgb.be.ordercore.match.dto.response;

import java.time.Instant;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.ordercore.match.dto.MatchGuideDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "경기 상세 조회 응답")
public record MatchDetailGetResponse(
		@Schema(description = "경기 ID", example = "1")
		Long matchId,
		@Schema(description = "경기 시작 시간 (UTC ISO-8601)", type = "string", example = "2026-03-28T05:00:00Z")
		Instant matchAt,
		@Schema(description = "판매 상태", example = "ON_SALE")
		SaleStatus saleStatus,
		@Schema(description = "홈 구단 정보")
		ClubDto homeClub,
		@Schema(description = "원정 구단 정보")
		ClubDto awayClub,
		@Schema(description = "경기 안내 정보")
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
