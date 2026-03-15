package com.goormgb.be.seat.recommendation.service;

import org.springframework.stereotype.Service;

import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.recommendation.dto.response.SeatEntryResponse;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatRecommendationService {

	private final MatchRepository matchRepository;
	private final SeatPreferenceRedisRepository seatPreferenceRedisRepository;

	public SeatEntryResponse getRecommendationSeatEntry(Long matchId, Long userId) {
		var match = matchRepository.findByIdOrThrow(matchId, ErrorCode.MATCH_NOT_FOUND);
		var seatSession = seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);

		return SeatEntryResponse.of(match, seatSession);
	}
}
