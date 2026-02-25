package com.goormgb.be.ordercore.match.service;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.club.repository.ClubRepository;
import com.goormgb.be.ordercore.match.dto.response.ClubMonthlyMatchesResponse;
import com.goormgb.be.ordercore.club.entity.Club;
import com.goormgb.be.ordercore.match.dto.response.MatchDetailGetResponse;
import com.goormgb.be.ordercore.match.dto.response.MatchListByDateResponse;
import com.goormgb.be.ordercore.match.entity.Match;
import com.goormgb.be.ordercore.match.repository.MatchRepository;
import com.goormgb.be.ordercore.match.utils.MatchDisplayUtils;
import com.goormgb.be.ordercore.match.utils.SalesOpenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {
    final private MatchRepository matchRepository;
    final private ClubRepository clubRepository;
    final private MatchDisplayUtils matchDisplayUtils;
    final private SalesOpenUtils salesOpenUtils;

    public MatchDetailGetResponse getMatchDetail(Long id){
        var match = matchRepository.findDetailByIdOrThrow(id);
        var matchGuide = matchDisplayUtils.toGuide(match);

        return MatchDetailGetResponse.of(match, matchGuide);
    }

    public MatchListByDateResponse getMatchesByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Match> matches = matchRepository.findAllByMatchAtGreaterThanEqualAndMatchAtLessThanOrderByMatchAtAsc(start, end);

        var summaries = matches.stream()
                .map(m -> MatchListByDateResponse.MatchSummary.of(m, salesOpenUtils.calculateSalesOpenAt(m)))
                .toList();

        return MatchListByDateResponse.of(date, summaries);
    }

    public ClubMonthlyMatchesResponse getClubMonthlyMatches(Long clubId, int year, int month){
        Preconditions.validate(clubRepository.existsById(clubId), ErrorCode.CLUB_NOT_FOUND);

        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

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
