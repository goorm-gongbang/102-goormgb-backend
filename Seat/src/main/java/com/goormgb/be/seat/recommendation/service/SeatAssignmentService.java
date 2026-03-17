package com.goormgb.be.seat.recommendation.service;

import org.springframework.stereotype.Service;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.recommendation.dto.response.SeatAssignmentResponse;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;
import com.goormgb.be.seat.redis.SeatSession;

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
 *   <li>트랜잭션 내에서 좌석 배정 + Hold 생성</li>
 *   <li>트랜잭션 커밋 후 락 해제</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SeatAssignmentService {

	private final SeatPreferenceRedisRepository seatPreferenceRedisRepository;
	private final BlockRepository blockRepository;
	private final SeatBlockLock seatBlockLock;
	private final SeatAssignmentTransactionalService seatAssignmentTransactionalService;

	public SeatAssignmentResponse assignAndHoldSeats(Long userId, Long matchId, Long blockId,
		boolean nearAdjacentToggle) {

		SeatSession seatSession = seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);
		int requiredSeats = seatSession.getTicketCount();

		Block block = blockRepository.findByIdWithSectionOrThrow(blockId);

		if (!seatBlockLock.tryLock(blockId)) {
			throw new CustomException(ErrorCode.SEAT_LOCK_ACQUISITION_FAILED);
		}

		try {
			return seatAssignmentTransactionalService.assignAndHold(
				userId, matchId, blockId, block, requiredSeats, nearAdjacentToggle);
		} finally {
			seatBlockLock.unlock(blockId);
		}
	}
}
