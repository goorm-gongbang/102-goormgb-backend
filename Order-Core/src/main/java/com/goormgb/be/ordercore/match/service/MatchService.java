package com.goormgb.be.ordercore.match.service;

import com.goormgb.be.ordercore.match.dto.response.MatchDetailGetResponse;
import com.goormgb.be.ordercore.match.repository.MatchRepository;
import com.goormgb.be.ordercore.match.utils.MatchDisplayUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {
    final private MatchRepository matchRepository;
    final private MatchDisplayUtils matchDisplayUtils;

    public MatchDetailGetResponse getMatchDetail(Long id){
        var match = matchRepository.findDetailByIdOrThrow(id);
        var matchGuide = matchDisplayUtils.toGuide(match);

        return MatchDetailGetResponse.of(match, matchGuide);
    }
}
