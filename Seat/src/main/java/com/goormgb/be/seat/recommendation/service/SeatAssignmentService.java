package com.goormgb.be.seat.recommendation.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.recommendation.dto.internal.SemiGroup;
import com.goormgb.be.seat.recommendation.dto.internal.SeatGroup;
import com.goormgb.be.seat.recommendation.dto.response.SeatAssignmentResponse;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;
import com.goormgb.be.seat.redis.SeatSession;
import com.goormgb.be.seat.seatHold.entity.SeatHold;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;

import lombok.RequiredArgsConstructor;

/**
 * 좌석 자동 배정 및 Hold 처리 서비스.
 *
 * <p>Redis 분산 락으로 동시성을 제어하며, 진짜 연석 → 준연석 fallback 순서로 좌석을 탐색한다.</p>
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li>SeatSession에서 ticketCount 조회</li>
 *   <li>Redis 분산 락 획득 (block_lock:{blockId})</li>
 *   <li>진짜 연석 탐색</li>
 *   <li>(fallback) nearAdjacentToggle이 true이면 준연석 탐색</li>
 *   <li>좌석 상태를 BLOCKED로 변경</li>
 *   <li>SeatHold 생성 (5분 TTL)</li>
 *   <li>락 해제</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SeatAssignmentService {

	private static final Duration HOLD_TTL = Duration.ofMinutes(5);

	private final SeatPreferenceRedisRepository seatPreferenceRedisRepository;
	private final BlockRepository blockRepository;
	private final SeatHoldRepository seatHoldRepository;
	private final RealConsecutiveFinder realConsecutiveFinder;
	private final SemiConsecutiveFinder semiConsecutiveFinder;
	private final SeatBlockLock seatBlockLock;
	private final Clock clock;

	public SeatAssignmentResponse assignAndHoldSeats(Long userId, Long matchId, Long blockId,
		boolean nearAdjacentToggle) {

		SeatSession seatSession = seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);
		int requiredSeats = seatSession.getTicketCount();

		Block block = blockRepository.findByIdWithSectionOrThrow(blockId);

		if (!seatBlockLock.tryLock(blockId)) {
			throw new CustomException(ErrorCode.SEAT_LOCK_ACQUISITION_FAILED);
		}

		try {
			return doAssignAndHold(userId, matchId, blockId, block, requiredSeats, nearAdjacentToggle);
		} finally {
			seatBlockLock.unlock(blockId);
		}
	}

	@Transactional
	protected SeatAssignmentResponse doAssignAndHold(
		Long userId, Long matchId, Long blockId, Block block,
		int requiredSeats, boolean nearAdjacentToggle
	) {
		// 기존 Hold 정리 (재요청 시)
		cleanupExistingHolds(userId, matchId);

		// 1. 진짜 연석 탐색
		var realResult = realConsecutiveFinder.findBestRealConsecutive(matchId, blockId, requiredSeats);

		if (realResult.isPresent()) {
			SeatGroup seatGroup = realResult.get();
			return holdSeats(userId, matchId, block, seatGroup.seats(), false);
		}

		// 2. 준연석 fallback (toggle이 켜져 있는 경우에만)
		if (nearAdjacentToggle) {
			var semiResult = semiConsecutiveFinder.findBestSemiConsecutive(matchId, blockId, requiredSeats);

			if (semiResult.isPresent()) {
				SemiGroup semiGroup = semiResult.get();
				return holdSeats(userId, matchId, block, semiGroup.allSeats(), true);
			}
		}

		throw new CustomException(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE);
	}

	private SeatAssignmentResponse holdSeats(
		Long userId, Long matchId, Block block,
		List<MatchSeat> seats, boolean semiConsecutive
	) {
		Instant expiresAt = clock.instant().plus(HOLD_TTL);

		seats.forEach(MatchSeat::markBlocked);

		List<SeatHold> holds = seats.stream()
			.map(seat -> SeatHold.builder()
				.matchSeatId(seat.getId())
				.matchId(matchId)
				.seatId(seat.getSeatId())
				.userId(userId)
				.expiresAt(expiresAt)
				.build())
			.toList();

		seatHoldRepository.saveAll(holds);

		return SeatAssignmentResponse.of(matchId, block, seats, expiresAt, semiConsecutive);
	}

	private void cleanupExistingHolds(Long userId, Long matchId) {
		List<SeatHold> existingHolds = seatHoldRepository.findAllByUserIdAndMatchId(userId, matchId);

		if (existingHolds.isEmpty()) {
			return;
		}

		seatHoldRepository.deleteAllByMatchSeatIdIn(
			existingHolds.stream().map(SeatHold::getMatchSeatId).toList()
		);
	}
}
