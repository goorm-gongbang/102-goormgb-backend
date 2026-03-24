package com.goormgb.be.seat.matchSeat.service;

import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.anyList;
import static org.mockito.Mockito.*;

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

@ExtendWith(MockitoExtension.class)
class MatchSeatCleanupServiceTest {

	private static final Instant NOW = Instant.parse("2026-03-23T16:00:00Z");
	private static final Instant EXPECTED_CUTOFF = Instant.parse("2026-03-16T15:00:00Z");

	@Mock
	private MatchSeatRepository matchSeatRepository;
	@Mock
	private Clock clock;

	@InjectMocks
	private MatchSeatCleanupService matchSeatCleanupService;

	@Test
	@DisplayName("삭제 가능한 match_seat이 없으면 삭제를 수행하지 않는다")
	void cleanupEndedMatchSeats_noTargets() {
		// given
		given(clock.instant()).willReturn(NOW);
		given(matchSeatRepository.findCleanupTargetMatchSeatIds("ENDED", EXPECTED_CUTOFF))
			.willReturn(List.of());

		// when
		matchSeatCleanupService.cleanupEndedMatchSeats();

		// then
		then(matchSeatRepository).should().findCleanupTargetMatchSeatIds("ENDED", EXPECTED_CUTOFF);
		then(matchSeatRepository).should(never()).deleteByIdIn(anyList());
	}

	@Test
	@DisplayName("7일이 지난 종료 경기의 match_seat을 삭제한다")
	void cleanupEndedMatchSeats_deletesTargets() {
		// given
		given(clock.instant()).willReturn(NOW);
		List<Long> cleanupTargetMatchSeatIds = List.of(11L, 12L, 13L);
		given(matchSeatRepository.findCleanupTargetMatchSeatIds("ENDED", EXPECTED_CUTOFF))
			.willReturn(cleanupTargetMatchSeatIds);
		given(matchSeatRepository.deleteByIdIn(cleanupTargetMatchSeatIds)).willReturn(3);

		// when
		matchSeatCleanupService.cleanupEndedMatchSeats();

		// then
		then(matchSeatRepository).should().findCleanupTargetMatchSeatIds("ENDED", EXPECTED_CUTOFF);
		then(matchSeatRepository).should().deleteByIdIn(cleanupTargetMatchSeatIds);
	}
}
