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

import lombok.RequiredArgsConstructor;

/**
 * 직접 선택 좌석 선점(Hold) 서비스.
 *
 * <p>유저가 좌석맵에서 직접 클릭한 좌석들을 선점한다.
 * 입력 검증과 Redisson 분산 락 관리를 담당하며,
 * 실제 트랜잭션 처리는 {@link SeatHoldTransactionalService}에 위임하여
 * 트랜잭션 커밋이 락 해제보다 먼저 완료되도록 보장한다.</p>
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li>seatIds 정규화 및 검증 (중복, null)</li>
 *   <li>좌석 단위 Redisson 분산 락 획득 (정렬 순서로 데드락 방지)</li>
 *   <li>트랜잭션 서비스에서 Hold 생성/갱신 + 커밋</li>
 *   <li>락 해제</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SeatHoldService {

	/** 1인당 1회 hold 최대 좌석 수 (경기당 최대 구매 수량과 동일) */
	private static final int MAX_HOLD_SEATS_PER_REQUEST = 8;

	private final SeatHoldLockManager seatHoldLockManager;
	private final SeatHoldTransactionalService seatHoldTransactionalService;

	public SeatHoldCreateResponse createOrRefreshHold(Long userId, Long matchId, List<Long> seatIds) {
		List<Long> normalizedSeatIds = normalizeSeatIds(seatIds);

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
				ErrorCode.INVALID_SEAT_HOLD_REQUEST);

		Preconditions.validate(
				seatIds.size() <= MAX_HOLD_SEATS_PER_REQUEST,
				ErrorCode.INVALID_SEAT_HOLD_REQUEST);

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
}
