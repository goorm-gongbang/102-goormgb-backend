package com.goormgb.be.seat.recommendation.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferenceRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferredBlockRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingViewpointPriorityRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.booking.repository.BookingOptionsRedisRepository;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.repository.BlockRemainingSeatProjection;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.metrics.SeatMetricsService;
import com.goormgb.be.seat.recommendation.dto.internal.BlockRecommendation;
import com.goormgb.be.seat.recommendation.dto.response.BlockRecommendationResponse;
import com.goormgb.be.seat.recommendation.dto.response.SeatEntryResponse;
import com.goormgb.be.seat.redis.SeatSession;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatRecommendationService {

	private static final int CONSECUTIVE_COUNT_THRESHOLD = 10;

	private final MatchRepository matchRepository;
	private final BookingOptionsRedisRepository bookingOptionsRedisRepository;
	private final BlockRepository blockRepository;
	private final MatchSeatRepository matchSeatRepository;
	private final OnboardingPreferredBlockRepository onboardingPreferredBlockRepository;
	private final OnboardingPreferenceRepository onboardingPreferenceRepository;
	private final OnboardingViewpointPriorityRepository onboardingViewpointPriorityRepository;
	private final ConsecutiveSeatCounter consecutiveSeatCounter;
	private final SemiConsecutiveSeatCounter semiConsecutiveSeatCounter;
	private final PreferenceScoreCalculator preferenceScoreCalculator;
	private final SeatMetricsService seatMetricsService;

	public SeatEntryResponse getRecommendationSeatEntry(Long matchId, Long userId) {
		var match = matchRepository.findDetailByIdOrThrow(matchId);
		var bookingOptions = bookingOptionsRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);
		var seatSession = SeatSession.from(bookingOptions);

		return SeatEntryResponse.of(match, seatSession);
	}

	@Transactional(readOnly = true)
	public BlockRecommendationResponse getRecommendedBlocks(Long matchId, Long userId) {
		long start = System.nanoTime();

		// 추천 좌석 요청 총 횟수 증가 (유입 트래픽 추적)
		seatMetricsService.increaseRecommendTotal();
		try {
			var bookingOptions = bookingOptionsRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);

			Preconditions.validate(bookingOptions.recommendationEnabled(), ErrorCode.BAD_REQUEST);
			Preconditions.validate(bookingOptions.ticketCount() != null, ErrorCode.INVALID_TICKET_COUNT);

			SeatSession seatSession = SeatSession.from(bookingOptions);
			int ticketCount = seatSession.getTicketCount();
			List<Long> preferredBlockNums = onboardingPreferredBlockRepository.findBlockIdsByUserId(userId);

			Match match = matchRepository.findDetailByIdOrThrow(matchId);
			List<Block> preferredBlocks = blockRepository.findAllByBlockNumInWithSectionAndArea(preferredBlockNums);
			OnboardingPreference pref = onboardingPreferenceRepository.findByUserIdOrThrow(
				userId, ErrorCode.PREFERENCE_NOT_FOUND);
			List<OnboardingViewpointPriority> viewpoints =
				onboardingViewpointPriorityRepository.findAllByUserIdOrderByPriorityAsc(userId);

			boolean nearAdjacentToggle = seatSession.isNearAdjacentToggle();
			List<BlockRecommendation> recommendations = buildRecommendations(
				matchId, ticketCount, preferredBlocks, nearAdjacentToggle);

			Preconditions.validate(!recommendations.isEmpty(), ErrorCode.NO_AVAILABLE_BLOCK);

			sortRecommendations(recommendations, pref, viewpoints, match, nearAdjacentToggle);

			// 추천 좌석 탐색 성공 횟수 증가
			seatMetricsService.increaseRecommendSuccess();

			return BlockRecommendationResponse.of(matchId, ticketCount, recommendations, nearAdjacentToggle);
		} catch (CustomException e) {
			// 추천 좌석 탐색 실패 횟수 증가 (예외 발생, 조건 불일치 등 실패 케이스 추적)
			seatMetricsService.increaseRecommendFail();
			throw e;
		} finally {
			// 추천 알고리즘 전체 실행 시간 기록 (성능 및 병목 분석용)
			seatMetricsService.recordProcessTime(Duration.ofNanos(System.nanoTime() - start));
		}

	}

	private List<BlockRecommendation> buildRecommendations(
		Long matchId, int ticketCount, List<Block> blocks, boolean nearAdjacentToggle
	) {
		List<Long> blockIds = blocks.stream().map(Block::getId).toList();

		Map<Long, Long> remainingMap = matchSeatRepository
			.countRemainingSeatsByMatchIdAndBlockIdIn(matchId, blockIds)
			.stream()
			.collect(Collectors.toMap(
				BlockRemainingSeatProjection::getBlockId,
				BlockRemainingSeatProjection::getRemainingSeatCount
			));

		List<BlockRecommendation> recommendations = new ArrayList<>();
		for (Block block : blocks) {
			// 블럭당 AVAILABLE 좌석을 1회 조회하여 real/semi 두 카운터에 재사용
			// (nearAdjacentToggle=true 시 각 카운터가 독립적으로 조회하면 2×N 쿼리 발생)
			List<MatchSeat> availableSeats =
				matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(matchId, block.getId());

			int realCount = consecutiveSeatCounter.countRealConsecutiveSeats(availableSeats, ticketCount);
			int semiCount = 0;

			if (nearAdjacentToggle) {
				semiCount = semiConsecutiveSeatCounter.countSemiConsecutiveSeats(availableSeats, ticketCount);
			}

			boolean included = nearAdjacentToggle
				? (realCount > 0 || semiCount > 0)
				: (realCount > 0);

			if (included) {
				long remaining = remainingMap.getOrDefault(block.getId(), 0L);
				recommendations.add(new BlockRecommendation(block, realCount, semiCount, remaining));
			}
		}

		return recommendations;
	}

	private void sortRecommendations(
		List<BlockRecommendation> recommendations,
		OnboardingPreference pref,
		List<OnboardingViewpointPriority> viewpoints,
		Match match,
		boolean nearAdjacentToggle
	) {
		recommendations.sort((b1, b2) -> {
			int count1 = nearAdjacentToggle ? b1.combinedCount() : b1.realConsecutiveCount();
			int count2 = nearAdjacentToggle ? b2.combinedCount() : b2.realConsecutiveCount();
			int countDiff = count2 - count1;

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
