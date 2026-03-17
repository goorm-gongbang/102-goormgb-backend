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
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.recommendation.dto.internal.SeatGroup;
import com.goormgb.be.seat.recommendation.dto.internal.SemiGroup;
import com.goormgb.be.seat.recommendation.dto.response.SeatAssignmentResponse;
import com.goormgb.be.seat.seatHold.entity.SeatHold;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;

import lombok.RequiredArgsConstructor;

/**
 * 좌석 자동 배정의 트랜잭션 전담 서비스.
 *
 * <p>{@link SeatAssignmentService}에서 분산 락을 획득한 뒤 호출되며,
 * 별도 빈으로 분리하여 {@code @Transactional} AOP 프록시가 정상 동작하고
 * 트랜잭션 커밋이 락 해제보다 먼저 완료되도록 보장한다.</p>
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li>기존 Hold 정리 (재요청 시)</li>
 *   <li>진짜 연석 탐색 ({@link RealConsecutiveFinder})</li>
 *   <li>(fallback) nearAdjacentToggle이 true이면 준연석 탐색 ({@link SemiConsecutiveFinder})</li>
 *   <li>좌석 상태를 BLOCKED로 변경</li>
 *   <li>SeatHold 생성 (5분 TTL)</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SeatAssignmentTransactionalService {

	private static final Duration HOLD_TTL = Duration.ofMinutes(5);

	private final MatchSeatRepository matchSeatRepository;
	private final SeatHoldRepository seatHoldRepository;
	private final RealConsecutiveFinder realConsecutiveFinder;
	private final SemiConsecutiveFinder semiConsecutiveFinder;
	private final Clock clock;

	/**
	 * 블럭 내 최적 연석을 탐색하여 좌석을 배정하고 Hold를 생성한다.
	 *
	 * <p>진짜 연석을 우선 탐색하고, 없으면 준연석으로 fallback한다.
	 * 이 메서드는 반드시 분산 락이 획득된 상태에서 호출되어야 한다.</p>
	 *
	 * @param userId              사용자 ID
	 * @param matchId             경기 ID
	 * @param blockId             블럭 ID
	 * @param block               블럭 엔티티 (섹션 정보 포함)
	 * @param requiredSeats       필요 좌석 수
	 * @param nearAdjacentToggle  준연석 허용 여부
	 * @return 배정된 좌석 정보 및 Hold 만료 시각
	 * @throws CustomException 연석/준연석 모두 없을 경우 {@code NO_CONSECUTIVE_SEAT_AVAILABLE}
	 */
	@Transactional
	public SeatAssignmentResponse assignAndHold(
		Long userId, Long matchId, Long blockId, Block block,
		int requiredSeats, boolean nearAdjacentToggle
	) {
		cleanupExistingHolds(userId, matchId);

		var realResult = realConsecutiveFinder.findBestRealConsecutive(matchId, blockId, requiredSeats);

		if (realResult.isPresent()) {
			SeatGroup seatGroup = realResult.get();
			return holdSeats(userId, matchId, block, seatGroup.seats(), false);
		}

		if (nearAdjacentToggle) {
			var semiResult = semiConsecutiveFinder.findBestSemiConsecutive(matchId, blockId, requiredSeats);

			if (semiResult.isPresent()) {
				SemiGroup semiGroup = semiResult.get();
				return holdSeats(userId, matchId, block, semiGroup.allSeats(), true);
			}
		}

		throw new CustomException(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE);
	}

	/**
	 * 탐색된 좌석들의 상태를 BLOCKED로 변경하고 SeatHold 레코드를 생성한다.
	 */
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

	/**
	 * 사용자의 기존 Hold를 정리한다.
	 *
	 * <p>재요청 시 이전 Hold의 좌석을 AVAILABLE로 복원하고 Hold 레코드를 삭제한다.
	 * delete 후 flush()를 호출하여 JPA flush 순서(INSERT → DELETE)로 인한
	 * unique constraint violation을 방지한다.</p>
	 */
	private void cleanupExistingHolds(Long userId, Long matchId) {
		List<SeatHold> existingHolds = seatHoldRepository.findAllByUserIdAndMatchId(userId, matchId);

		if (existingHolds.isEmpty()) {
			return;
		}

		List<Long> matchSeatIds = existingHolds.stream().map(SeatHold::getMatchSeatId).toList();

		List<MatchSeat> seatsToRelease = matchSeatRepository.findAllById(matchSeatIds);
		seatsToRelease.forEach(MatchSeat::markAvailable);

		seatHoldRepository.deleteAllByMatchSeatIdIn(matchSeatIds);
		seatHoldRepository.flush();
	}
}
