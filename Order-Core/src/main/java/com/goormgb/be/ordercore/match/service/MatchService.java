package com.goormgb.be.ordercore.match.service;

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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {
    final private MatchRepository matchRepository;
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
}
