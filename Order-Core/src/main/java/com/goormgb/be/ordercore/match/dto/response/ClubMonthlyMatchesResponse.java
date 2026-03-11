package com.goormgb.be.ordercore.match.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.domain.match.enums.SaleStatus;

public record ClubMonthlyMatchesResponse(
	Long clubId,
	int year,
	int month,
	int totalMatchCount,
	List<MatchItem> matches
) {
	public static ClubMonthlyMatchesResponse of(Long clubId, int year, int month, List<MatchItem> items) {
		return new ClubMonthlyMatchesResponse(clubId, year, month, items.size(), items);
	}

	public record MatchItem(
		Long matchId,
		Instant matchAt,
		OpponentClub opponentClub,
		SaleStatus saleStatus,
		boolean isHomeMatch
	) {
	}

	public record OpponentClub(
		Long clubId,
		String koName,
		String logoImg
	) {
	}
}
