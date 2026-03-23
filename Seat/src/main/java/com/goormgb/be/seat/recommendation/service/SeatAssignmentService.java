package com.goormgb.be.seat.recommendation.service;

import org.springframework.stereotype.Service;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.booking.model.BookingOptions;
import com.goormgb.be.seat.booking.repository.BookingOptionsRedisRepository;
import com.goormgb.be.seat.recommendation.dto.response.SeatAssignmentResponse;

import lombok.RequiredArgsConstructor;

/**
 * 좌석 자동 배정 및 Hold 처리 서비스.
 *
 * <p>Redis 분산 락으로 동시성을 제어하며, 진짜 연석 → 준연석 fallback 순서로 좌석을 탐색한다.</p>
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li>BookingOptions에서 ticketCount, nearAdjacentToggle 조회</li>
 *   <li>Redis 분산 락 획득 (seat:recommendation:match:{matchId}:block:{blockId})</li>
 *   <li>트랜잭션 내에서 좌석 배정 + Hold 생성</li>
 *   <li>트랜잭션 커밋 후 락 해제</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SeatAssignmentService {

	private final BookingOptionsRedisRepository bookingOptionsRedisRepository;
	private final BlockRepository blockRepository;
	private final SeatBlockLock seatBlockLock;
	private final SeatAssignmentTransactionalService seatAssignmentTransactionalService;

	public SeatAssignmentResponse assignAndHoldSeats(Long userId, Long matchId, String blockCode) {

		BookingOptions bookingOptions = bookingOptionsRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);

		Preconditions.validate(bookingOptions.recommendationEnabled(), ErrorCode.BAD_REQUEST);
		Preconditions.validate(bookingOptions.ticketCount() != null, ErrorCode.INVALID_TICKET_COUNT);

		Integer requiredSeats = bookingOptions.ticketCount();
		boolean nearAdjacentToggle = bookingOptions.nearAdjacentToggle();

		Block block = blockRepository.findByBlockCodeWithSectionOrThrow(blockCode);
		Long blockId = block.getId();

		Preconditions.validate(seatBlockLock.tryLock(matchId, blockId), ErrorCode.SEAT_LOCK_ACQUISITION_FAILED);

		try {
			return seatAssignmentTransactionalService.assignAndHold(
				userId, matchId, blockId, block, requiredSeats, nearAdjacentToggle);
		} finally {
			seatBlockLock.unlock(matchId, blockId);
		}
	}
}
