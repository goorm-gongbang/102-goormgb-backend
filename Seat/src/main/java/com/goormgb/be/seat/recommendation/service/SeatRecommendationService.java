package com.goormgb.be.seat.recommendation.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferenceRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingViewpointPriorityRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.recommendation.dto.internal.BlockRecommendation;
import com.goormgb.be.seat.recommendation.dto.response.BlockRecommendationResponse;
import com.goormgb.be.seat.recommendation.dto.response.SeatEntryResponse;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;
import com.goormgb.be.seat.redis.SeatSession;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatRecommendationService {

	private static final int CONSECUTIVE_COUNT_THRESHOLD = 10;

	private final MatchRepository matchRepository;
	private final SeatPreferenceRedisRepository seatPreferenceRedisRepository;
	private final BlockRepository blockRepository;
	private final OnboardingPreferenceRepository onboardingPreferenceRepository;
	private final OnboardingViewpointPriorityRepository onboardingViewpointPriorityRepository;
	private final ConsecutiveSeatCounter consecutiveSeatCounter;
	private final PreferenceScoreCalculator preferenceScoreCalculator;

	public SeatEntryResponse getRecommendationSeatEntry(Long matchId, Long userId) {
		var match = matchRepository.findDetailByIdOrThrow(matchId);
		var seatSession = seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);

		return SeatEntryResponse.of(match, seatSession);
	}

	@Transactional(readOnly = true)
	public BlockRecommendationResponse getRecommendedBlocks(Long matchId, Long userId) {
		SeatSession seatSession = seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);
		int ticketCount = seatSession.getTicketCount();
		List<Long> preferredBlockIds = seatSession.getPreferredBlockIds();

		Match match = matchRepository.findDetailByIdOrThrow(matchId);
		List<Block> preferredBlocks = blockRepository.findAllByIdInWithSectionAndArea(preferredBlockIds);
		OnboardingPreference pref = onboardingPreferenceRepository.findByUserIdOrThrow(
			userId, ErrorCode.PREFERENCE_NOT_FOUND);
		List<OnboardingViewpointPriority> viewpoints =
			onboardingViewpointPriorityRepository.findAllByUserIdOrderByPriorityAsc(userId);

		List<BlockRecommendation> recommendations = buildRecommendations(matchId, ticketCount, preferredBlocks);

		if (recommendations.isEmpty()) {
			throw new CustomException(ErrorCode.NO_AVAILABLE_BLOCK);
		}

		sortRecommendations(recommendations, pref, viewpoints, match);

		return BlockRecommendationResponse.of(matchId, ticketCount, recommendations);
	}

	private List<BlockRecommendation> buildRecommendations(Long matchId, int ticketCount, List<Block> blocks) {
		List<BlockRecommendation> recommendations = new ArrayList<>();

		for (Block block : blocks) {
			int count = consecutiveSeatCounter.countRealConsecutiveSeats(matchId, block.getId(), ticketCount);
			if (count > 0) {
				recommendations.add(new BlockRecommendation(block, count));
			}
		}

		return recommendations;
	}

	private void sortRecommendations(
		List<BlockRecommendation> recommendations,
		OnboardingPreference pref,
		List<OnboardingViewpointPriority> viewpoints,
		Match match
	) {
		recommendations.sort((b1, b2) -> {
			int countDiff = b2.realConsecutiveCount() - b1.realConsecutiveCount();

			if (Math.abs(countDiff) > CONSECUTIVE_COUNT_THRESHOLD) {
				return countDiff;
			}

			int score1 = preferenceScoreCalculator.calculatePreferenceScore(b1.block(), pref, viewpoints, match);
			int score2 = preferenceScoreCalculator.calculatePreferenceScore(b2.block(), pref, viewpoints, match);

			if (score1 != score2) {
				return score2 - score1;
			}

			return countDiff;
		});
	}
}
