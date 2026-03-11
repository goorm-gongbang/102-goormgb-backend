package com.goormgb.be.ordercore.match.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.domain.match.enums.SaleStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구단 월별 경기 일정 조회 응답")
public record ClubMonthlyMatchesResponse(
	@Schema(description = "구단 ID", example = "1")
	Long clubId,
	@Schema(description = "조회 연도", example = "2026")
	int year,
	@Schema(description = "조회 월", example = "3")
	int month,
	@Schema(description = "해당 월 전체 경기 수", example = "12")
	int totalMatchCount,
	@Schema(description = "경기 목록")
	List<MatchItem> matches
) {
	public static ClubMonthlyMatchesResponse of(Long clubId, int year, int month, List<MatchItem> items) {
		return new ClubMonthlyMatchesResponse(clubId, year, month, items.size(), items);
	}

	@Schema(description = "경기 항목")
	public record MatchItem(
		@Schema(description = "경기 ID", example = "1")
		Long matchId,
		@Schema(description = "경기 시작 시간 (UTC ISO-8601)", type = "string", example = "2026-03-28T05:00:00Z")
		Instant matchAt,
		@Schema(description = "상대 구단 정보")
		OpponentClub opponentClub,
		@Schema(description = "판매 상태", example = "ON_SALE")
		SaleStatus saleStatus,
		@Schema(description = "홈 경기 여부", example = "true")
		boolean isHomeMatch
	) {
	}

	@Schema(description = "상대 구단 정보")
	public record OpponentClub(
			@Schema(description = "구단 ID", example = "2")
			Long clubId,
			@Schema(description = "구단 한글 이름", example = "두산 베어스")
			String koName,
			@Schema(description = "구단 로고 이미지 URL", example = "https://example.com/logo.png")
			String logoImg
	) {
	}
}
