package com.goormgb.be.seat.seatHold.scheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 만료된 SeatHold를 주기적으로 정리하는 스케줄러.
 *
 * <p>1분 간격으로 실행되며, 만료된 Hold의 좌석을 AVAILABLE로 복원하고
 * Hold 레코드를 삭제한다. SOLD 상태 좌석은 영향받지 않는다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldCleanupScheduler {

	private final SeatHoldRepository seatHoldRepository;
	private final MatchSeatRepository matchSeatRepository;
	private final Clock clock;

	@Scheduled(fixedDelay = 60_000)
	@Transactional
	public void cleanupExpiredHolds() {
		Instant now = clock.instant();

		List<Long> expiredMatchSeatIds = seatHoldRepository.findExpiredMatchSeatIds(now);

		if (expiredMatchSeatIds.isEmpty()) {
			return;
		}

		int restoredCount = matchSeatRepository.markAvailableIfBlockedInBatch(expiredMatchSeatIds);
		int deletedCount = seatHoldRepository.deleteExpiredHolds(now);

		log.info("만료 Hold 정리 완료 - 좌석 복원: {}건, Hold 삭제: {}건", restoredCount, deletedCount);
	}
}
