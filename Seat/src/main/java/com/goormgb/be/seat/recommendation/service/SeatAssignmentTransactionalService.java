package com.goormgb.be.seat.recommendation.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import lombok.extern.slf4j.Slf4j;

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
 *   <li>진짜 연석 탐색 → 충돌 시 재탐색 (최대 {@value #MAX_RETRY}회)</li>
 *   <li>(fallback) 준연석 탐색 → 충돌 시 재탐색 (최대 {@value #MAX_RETRY}회)</li>
 *   <li>조건부 UPDATE로 좌석 상태를 BLOCKED로 변경 (일반 유저 충돌 감지)</li>
 *   <li>SeatHold 생성 (5분 TTL)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatAssignmentTransactionalService {

	private static final Duration HOLD_TTL = Duration.ofMinutes(5);
	private static final int MAX_RETRY = 3;

	private final MatchSeatRepository matchSeatRepository;
	private final SeatHoldRepository seatHoldRepository;
	private final RealConsecutiveFinder realConsecutiveFinder;
	private final SemiConsecutiveFinder semiConsecutiveFinder;
	private final Clock clock;

	/**
	 * 블럭 내 최적 연석을 탐색하여 좌석을 배정하고 Hold를 생성한다.
	 *
	 * <p>진짜 연석을 우선 탐색하고, 충돌 발생 시 다른 연석 구간을 재탐색한다.
	 * 진짜 연석이 모두 소진되면 준연석으로 fallback하며, 준연석도 동일하게 재탐색한다.
	 * 이 메서드는 반드시 분산 락이 획득된 상태에서 호출되어야 한다.</p>
	 *
	 * @param userId              사용자 ID
	 * @param matchId             경기 ID
	 * @param blockId             블럭 ID
	 * @param block               블럭 엔티티 (섹션 정보 포함)
	 * @param requiredSeats       필요 좌석 수
	 * @param nearAdjacentToggle  준연석 허용 여부
	 * @return 배정된 좌석 정보 및 Hold 만료 시각
	 * @throws CustomException 연석/준연석 모두 없거나 재시도 소진 시 {@code NO_CONSECUTIVE_SEAT_AVAILABLE}
	 */
	@Transactional
	public SeatAssignmentResponse assignAndHold(
		Long userId, Long matchId, Long blockId, Block block,
		int requiredSeats, boolean nearAdjacentToggle
	) {
		cleanupExistingHolds(userId, matchId);

		// 1. 진짜 연석 탐색 + 충돌 시 재탐색
		for (int retry = 0; retry < MAX_RETRY; retry++) {
			var realResult = realConsecutiveFinder.findBestRealConsecutive(matchId, blockId, requiredSeats);

			if (realResult.isEmpty()) {
				break;
			}

			SeatGroup seatGroup = realResult.get();
			var response = tryHoldSeats(userId, matchId, block, seatGroup.seats(), false);

			if (response.isPresent()) {
				return response.get();
			}

			log.info("진짜 연석 충돌 발생 - matchId: {}, blockId: {}, retry: {}/{}", matchId, blockId, retry + 1,
				MAX_RETRY);
		}

		// 2. 준연석 탐색 + 충돌 시 재탐색
		if (nearAdjacentToggle) {
			for (int retry = 0; retry < MAX_RETRY; retry++) {
				var semiResult = semiConsecutiveFinder.findBestSemiConsecutive(matchId, blockId, requiredSeats);

				if (semiResult.isEmpty()) {
					break;
				}

				SemiGroup semiGroup = semiResult.get();
				var response = tryHoldSeats(userId, matchId, block, semiGroup.allSeats(), true);

				if (response.isPresent()) {
					return response.get();
				}

				log.info("준연석 충돌 발생 - matchId: {}, blockId: {}, retry: {}/{}", matchId, blockId, retry + 1,
					MAX_RETRY);
			}
		}

		throw new CustomException(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE);
	}

	/**
	 * 조건부 UPDATE로 좌석 선점을 시도한다.
	 *
	 * <p>{@code markBlockedIfAvailable}을 사용하여 AVAILABLE 상태인 좌석만 변경한다.
	 * 일반 유저가 먼저 선점한 좌석이 포함된 경우 이미 변경한 좌석을 롤백하고
	 * {@code Optional.empty()}를 반환하여 호출부에서 재탐색하도록 유도한다.</p>
	 *
	 * @return 성공 시 배정 응답, 충돌 시 Optional.empty()
	 */
	private Optional<SeatAssignmentResponse> tryHoldSeats(
		Long userId, Long matchId, Block block,
		List<MatchSeat> seats, boolean semiConsecutive
	) {
		Instant expiresAt = clock.instant().plus(HOLD_TTL);

		List<MatchSeat> blockedSeats = new ArrayList<>();
		for (MatchSeat seat : seats) {
			int updated = matchSeatRepository.markBlockedIfAvailable(seat.getId());
			if (updated == 0) {
				rollbackBlockedSeats(blockedSeats);
				return Optional.empty();
			}
			blockedSeats.add(seat);
		}

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

		return Optional.of(SeatAssignmentResponse.of(matchId, block, seats, expiresAt, semiConsecutive));
	}

	/**
	 * 충돌 감지 시 이미 BLOCKED로 변경한 좌석들을 AVAILABLE로 일괄 롤백한다.
	 */
	private void rollbackBlockedSeats(List<MatchSeat> blockedSeats) {
		if (blockedSeats.isEmpty()) {
			return;
		}
		List<Long> idsToRollback = blockedSeats.stream()
			.map(MatchSeat::getId)
			.toList();
		matchSeatRepository.markAvailableIfBlockedInBatch(idsToRollback);
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
