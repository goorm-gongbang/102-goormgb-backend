package com.goormgb.be.seat.seatHold.scheduler;

import static org.mockito.BDDMockito.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;

@ExtendWith(MockitoExtension.class)
class SeatHoldCleanupSchedulerTest {

	@Mock
	private SeatHoldRepository seatHoldRepository;
	@Mock
	private MatchSeatRepository matchSeatRepository;
	@Mock
	private Clock clock;

	@InjectMocks
	private SeatHoldCleanupScheduler seatHoldCleanupScheduler;

	private static final Instant NOW = Instant.parse("2026-03-17T12:00:00Z");

	@Test
	@DisplayName("만료된 Hold가 있으면 좌석 복원 후 Hold를 삭제한다")
	void 만료된_Hold_정리() {
		// given
		given(clock.instant()).willReturn(NOW);

		List<Long> expiredMatchSeatIds = List.of(100L, 200L, 300L);
		given(seatHoldRepository.findExpiredMatchSeatIds(NOW)).willReturn(expiredMatchSeatIds);
		given(matchSeatRepository.markAvailableIfBlockedInBatch(expiredMatchSeatIds)).willReturn(3);
		given(seatHoldRepository.deleteExpiredHolds(NOW)).willReturn(3);

		// when
		seatHoldCleanupScheduler.cleanupExpiredHolds();

		// then
		then(matchSeatRepository).should().markAvailableIfBlockedInBatch(expiredMatchSeatIds);
		then(seatHoldRepository).should().deleteExpiredHolds(NOW);
	}

	@Test
	@DisplayName("만료된 Hold가 없으면 아무 작업도 수행하지 않는다")
	void 만료된_Hold_없음() {
		// given
		given(clock.instant()).willReturn(NOW);
		given(seatHoldRepository.findExpiredMatchSeatIds(NOW)).willReturn(List.of());

		// when
		seatHoldCleanupScheduler.cleanupExpiredHolds();

		// then
		then(matchSeatRepository).should(never()).markAvailableIfBlockedInBatch(any());
		then(seatHoldRepository).should(never()).deleteExpiredHolds(any());
	}

	@Test
	@DisplayName("SOLD 상태 좌석은 BLOCKED만 복원하므로 영향받지 않는다")
	void SOLD_좌석_영향없음() {
		// given
		given(clock.instant()).willReturn(NOW);

		List<Long> expiredMatchSeatIds = List.of(100L, 200L);
		given(seatHoldRepository.findExpiredMatchSeatIds(NOW)).willReturn(expiredMatchSeatIds);
		// 2개 중 1개만 BLOCKED → 1개만 복원됨
		given(matchSeatRepository.markAvailableIfBlockedInBatch(expiredMatchSeatIds)).willReturn(1);
		given(seatHoldRepository.deleteExpiredHolds(NOW)).willReturn(2);

		// when
		seatHoldCleanupScheduler.cleanupExpiredHolds();

		// then
		then(matchSeatRepository).should().markAvailableIfBlockedInBatch(expiredMatchSeatIds);
		then(seatHoldRepository).should().deleteExpiredHolds(NOW);
	}
}
