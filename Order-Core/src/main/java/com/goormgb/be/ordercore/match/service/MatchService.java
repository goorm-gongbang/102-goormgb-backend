package com.goormgb.be.ordercore.match.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.club.repository.ClubRepository;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.match.dto.response.ClubMonthlyMatchesResponse;
import com.goormgb.be.ordercore.match.dto.response.MatchDetailGetResponse;
import com.goormgb.be.ordercore.match.dto.response.MatchListByDateResponse;
import com.goormgb.be.ordercore.match.utils.MatchDisplayUtils;
import com.goormgb.be.ordercore.match.utils.SalesOpenUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {
	private final MatchRepository matchRepository;
	private final ClubRepository clubRepository;
	private final MatchDisplayUtils matchDisplayUtils;
	private final SalesOpenUtils salesOpenUtils;

	public MatchDetailGetResponse getMatchDetail(Long id) {
		var match = matchRepository.findDetailByIdOrThrow(id);
		var matchGuide = matchDisplayUtils.toGuide(match);

		return MatchDetailGetResponse.of(match, matchGuide);
	}

	public MatchListByDateResponse getMatchesByDate(LocalDate date) {
		Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

		List<Match> matches = matchRepository.findAllByMatchAtGreaterThanEqualAndMatchAtLessThanOrderByMatchAtAsc(start,
			end);

		var summaries = matches.stream()
			.map(m -> MatchListByDateResponse.MatchSummary.of(m, salesOpenUtils.calculateSalesOpenAt(m)))
			.toList();

		return MatchListByDateResponse.of(date, summaries);
	}

	public ClubMonthlyMatchesResponse getClubMonthlyMatches(Long clubId, int year, int month) {
		Preconditions.validate(clubRepository.existsById(clubId), ErrorCode.CLUB_NOT_FOUND);
		Preconditions.validate(month >= 1 && month <= 12, ErrorCode.INVALID_MATCH_MONTH);
		Preconditions.validate(year >= 1900 && year <= 2100, ErrorCode.INVALID_MATCH_YEAR);

		YearMonth ym = YearMonth.of(year, month);
		Instant start = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

		List<Match> matches = matchRepository.findMonthlyByClubId(clubId, start, end);

		List<ClubMonthlyMatchesResponse.MatchItem> items = matches.stream()
			.map(m -> toItem(clubId, m))
			.toList();

		return ClubMonthlyMatchesResponse.of(clubId, year, month, items);
	}

	private ClubMonthlyMatchesResponse.MatchItem toItem(Long clubId, Match m) {
		boolean isHome = m.getHomeClub().getId().equals(clubId);
		Club opponent = isHome ? m.getAwayClub() : m.getHomeClub();

		return new ClubMonthlyMatchesResponse.MatchItem(
			m.getId(),
			m.getMatchAt(),
			new ClubMonthlyMatchesResponse.OpponentClub(
				opponent.getId(),
				opponent.getKoName(),
				opponent.getLogoImg()
			),
			m.getSaleStatus(),
			isHome
		);
	}
}
