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

@Service
@RequiredArgsConstructor
public class SeatHoldTransactionalService {

	private static final Duration HOLD_TTL = Duration.ofMinutes(5);

	private final MatchSeatRepository matchSeatRepository;
	private final SeatHoldRepository seatHoldRepository;
	private final Clock clock;

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
