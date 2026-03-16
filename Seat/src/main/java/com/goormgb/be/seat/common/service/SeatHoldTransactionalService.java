package com.goormgb.be.seat.common.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.seat.common.dto.response.SeatHoldCreateResponse;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.seatHold.entity.SeatHold;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;

import lombok.RequiredArgsConstructor;

/**
 * 직접 선택 좌석 선점의 트랜잭션 전담 서비스.
 *
 * <p>{@link SeatHoldService}에서 분산 락을 획득한 뒤 호출되며,
 * 별도 빈으로 분리하여 {@code @Transactional} AOP 프록시가 정상 동작하고
 * 트랜잭션 커밋이 락 해제보다 먼저 완료되도록 보장한다.</p>
 *
 * <h3>Hold 생성/갱신 로직</h3>
 * <ul>
 *   <li>동일 좌석 재요청: 기존 Hold의 만료 시간만 연장 (5분 TTL 갱신)</li>
 *   <li>다른 좌석 요청: 기존 Hold 해제 → 신규 Hold 생성</li>
 *   <li>타 사용자 Hold 충돌: 예외 발생</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SeatHoldTransactionalService {

	private static final Duration HOLD_TTL = Duration.ofMinutes(5);

	private final MatchSeatRepository matchSeatRepository;
	private final SeatHoldRepository seatHoldRepository;
	private final Clock clock;

	/**
	 * 좌석 Hold를 생성하거나 기존 Hold를 갱신한다.
	 *
	 * <p>이 메서드는 반드시 분산 락이 획득된 상태에서 호출되어야 한다.</p>
	 *
	 * @param userId  사용자 ID
	 * @param matchId 경기 ID
	 * @param seatIds 정규화된 좌석 ID 목록 (정렬, 중복 제거 완료)
	 * @return Hold 생성 결과 (좌석 목록, 만료 시각)
	 * @throws CustomException 좌석 미존재, 판매 완료, 타 사용자 선점 시
	 */
	@Transactional
	public SeatHoldCreateResponse createOrRefreshHold(Long userId, Long matchId, List<Long> seatIds) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(HOLD_TTL);

		List<MatchSeat> requestedSeats = matchSeatRepository.findAllByMatchIdAndSeatIdIn(matchId, seatIds);

		Preconditions.validate(requestedSeats.size() == seatIds.size(), ErrorCode.MATCH_SEAT_NOT_FOUND);
		Preconditions.validate(
			requestedSeats.stream().noneMatch(seat -> seat.getSaleStatus() == MatchSeatSaleStatus.SOLD),
			ErrorCode.SEAT_ALREADY_SOLD
		);

		List<SeatHold> activeRequestedHolds = seatHoldRepository
			.findAllByMatchIdAndSeatIdInAndExpiresAtAfter(matchId, seatIds, now);

		boolean hasOtherUserHold = activeRequestedHolds.stream().anyMatch(hold -> !hold.isOwnedBy(userId));
		Preconditions.validate(!hasOtherUserHold, ErrorCode.SEAT_ALREADY_HELD_BY_OTHER);

		List<SeatHold> userActiveHolds = seatHoldRepository.findAllByUserIdAndMatchIdAndExpiresAtAfter(userId, matchId,
			now);
		Set<Long> currentHeldSeatIds = userActiveHolds.stream()
			.map(SeatHold::getSeatId)
			.collect(Collectors.toSet());
		Set<Long> requestedSeatSet = new HashSet<>(seatIds);

		if (currentHeldSeatIds.equals(requestedSeatSet) && !userActiveHolds.isEmpty()) {
			userActiveHolds.forEach(hold -> hold.extendHold(expiresAt));
			requestedSeats.forEach(MatchSeat::markBlocked);
			return SeatHoldCreateResponse.of(matchId, seatIds, expiresAt);
		}

		releaseUserActiveHolds(userActiveHolds);

		List<SeatHold> newHolds = requestedSeats.stream()
			.map(seat -> SeatHold.builder()
				.matchSeatId(seat.getId())
				.matchId(matchId)
				.seatId(seat.getSeatId())
				.userId(userId)
				.expiresAt(expiresAt)
				.build())
			.toList();

		requestedSeats.forEach(MatchSeat::markBlocked);
		seatHoldRepository.saveAll(newHolds);

		return SeatHoldCreateResponse.of(matchId, seatIds, expiresAt);
	}

	/**
	 * 사용자의 활성 Hold를 해제한다.
	 *
	 * <p>좌석 상태를 AVAILABLE로 복원하고 Hold 레코드를 삭제한다.
	 * delete 후 flush()를 호출하여 JPA flush 순서(INSERT → DELETE)로 인한
	 * unique constraint violation을 방지한다.</p>
	 */
	private void releaseUserActiveHolds(List<SeatHold> userActiveHolds) {
		if (userActiveHolds.isEmpty()) {
			return;
		}

		List<Long> matchSeatIds = userActiveHolds.stream().map(SeatHold::getMatchSeatId).toList();
		List<MatchSeat> seatsToRelease = matchSeatRepository.findAllById(matchSeatIds);
		seatsToRelease.forEach(MatchSeat::markAvailable);
		seatHoldRepository.deleteAllByMatchSeatIdIn(matchSeatIds);
		seatHoldRepository.flush();
	}
}
