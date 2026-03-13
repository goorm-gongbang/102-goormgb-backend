package com.goormgb.be.seat.matchSeat.scheduler;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.seat.matchSeat.service.MatchSeatPreparationService;

@ExtendWith(MockitoExtension.class)
class MatchSeatSchedulerTest {

	@Mock
	private MatchSeatPreparationService matchSeatPreparationService;

	@InjectMocks
	private MatchSeatScheduler matchSeatScheduler;

	@Test
	@DisplayName("스케줄러 실행 시 matchSeatPreparationService를 호출한다")
	void prepareMatchSeats_callsService() {
		// when
		matchSeatScheduler.prepareMatchSeats();

		// then
		verify(matchSeatPreparationService, times(1)).prepareMatchSeats();
	}
}
