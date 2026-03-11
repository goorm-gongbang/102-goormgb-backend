package com.goormgb.be.ordercore.match.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.statium.entity.Stadium;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "날짜별 경기 목록 조회 응답")
public record MatchListByDateResponse(
		@Schema(description = "조회 날짜", type = "string", example = "2026-03-28")
		LocalDate date,
		@Schema(description = "경기 수", example = "5")
		int matchCount,
		@Schema(description = "경기 목록")
		List<MatchSummary> matches
) {
	public static MatchListByDateResponse of(LocalDate date, List<MatchSummary> matches) {
		return new MatchListByDateResponse(date, matches.size(), matches);
	}

	@Schema(description = "경기 요약 정보")
	public record MatchSummary(
			@Schema(description = "경기 ID", example = "1")
			Long matchId,
			@Schema(description = "경기 시작 시간 (UTC ISO-8601)", type = "string", example = "2026-03-28T05:00:00Z")
			Instant matchAt,
			@Schema(description = "판매 상태", example = "ON_SALE")
			SaleStatus saleStatus,
			@Schema(description = "티켓 판매 오픈 시간 (UTC ISO-8601)", type = "string", example = "2026-03-21T02:00:00Z")
			Instant salesOpenAt,
			@Schema(description = "홈 구단 정보")
			ClubDto homeClub,
			@Schema(description = "원정 구단 정보")
			ClubDto awayClub,
			@Schema(description = "경기장 정보")
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