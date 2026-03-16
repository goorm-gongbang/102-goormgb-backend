package com.goormgb.be.seat.common.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.seat.common.dto.response.SeatHoldCreateResponse;
import com.goormgb.be.seat.common.service.lock.SeatHoldLockManager;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;
import com.goormgb.be.seat.redis.SeatSession;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

	private final SeatPreferenceRedisRepository seatPreferenceRedisRepository;
	private final SeatHoldLockManager seatHoldLockManager;
	private final SeatHoldTransactionalService seatHoldTransactionalService;

	public SeatHoldCreateResponse createOrRefreshHold(Long userId, Long matchId, List<Long> seatIds) {
		List<Long> normalizedSeatIds = normalizeSeatIds(seatIds);
		validateSeatCount(userId, matchId, normalizedSeatIds.size());

		List<RLock> locks = seatHoldLockManager.lockAll(matchId, normalizedSeatIds);
		try {
			return seatHoldTransactionalService.createOrRefreshHold(userId, matchId, normalizedSeatIds);
		} finally {
			seatHoldLockManager.unlockAll(locks);
		}
	}

	private List<Long> normalizeSeatIds(List<Long> seatIds) {
		Preconditions.validate(
			seatIds != null && !seatIds.isEmpty(),
			ErrorCode.INVALID_SEAT_HOLD_REQUEST
		);

		Preconditions.validate(
			seatIds.stream().noneMatch(Objects::isNull),
			ErrorCode.INVALID_SEAT_HOLD_REQUEST
		);

		Preconditions.validate(
			new HashSet<>(seatIds).size() == seatIds.size(),
			ErrorCode.INVALID_SEAT_HOLD_REQUEST
		);

		return seatIds.stream()
			.sorted(Comparator.naturalOrder())
			.toList();
	}

	private void validateSeatCount(Long userId, Long matchId, int requestedCount) {
		SeatSession seatSession =
			seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);

		Preconditions.validate(
			seatSession.getTicketCount() == requestedCount,
			ErrorCode.INVALID_SEAT_HOLD_REQUEST
		);
	}
}
