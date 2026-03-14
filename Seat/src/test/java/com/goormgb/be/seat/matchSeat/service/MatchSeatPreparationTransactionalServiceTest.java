package com.goormgb.be.seat.matchSeat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.seat.dto.SeatTemplateProjection;
import com.goormgb.be.seat.seat.enums.SeatZone;

@ExtendWith(MockitoExtension.class)
class MatchSeatPreparationTransactionalServiceTest {

	@Mock
	private MatchSeatRepository matchSeatRepository;

	@InjectMocks
	private MatchSeatPreparationTransactionalService transactionalService;

	@Test
	@DisplayName("이미 match seat가 존재하면 생성하지 않고 false를 반환한다")
	void prepareSingleMatchSeats_returnsFalseWhenAlreadyExists() {
		// given
		Long matchId = 1L;
		when(matchSeatRepository.existsByMatchId(matchId)).thenReturn(true);

		// when
		boolean result = transactionalService.prepareSingleMatchSeats(matchId, List.of());

		// then
		assertThat(result).isFalse();
		verify(matchSeatRepository, never()).saveAll(anyList());
	}

	@Test
	@DisplayName("템플릿 수가 2500개면 1000개 단위로 3번 saveAll 호출한다")
	void prepareSingleMatchSeats_savesInBatches() {
		// given
		Long matchId = 1L;
		when(matchSeatRepository.existsByMatchId(matchId)).thenReturn(false);

		List<SeatTemplateProjection> templates = new ArrayList<>();
		for (long i = 1; i <= 2500; i++) {
			templates.add(new FakeSeatTemplateProjection(i));
		}

		// when
		boolean result = transactionalService.prepareSingleMatchSeats(matchId, templates);

		// then
		assertThat(result).isTrue();
		verify(matchSeatRepository, times(3)).saveAll(anyList());
	}

	private static class FakeSeatTemplateProjection implements SeatTemplateProjection {
		private final Long seatId;

		private FakeSeatTemplateProjection(Long seatId) {
			this.seatId = seatId;
		}

		@Override
		public Long getSeatId() {
			return seatId;
		}

		@Override
		public Long getAreaId() {
			return 10L;
		}

		@Override
		public Long getSectionId() {
			return 20L;
		}

		@Override
		public Long getBlockId() {
			return 30L;
		}

		@Override
		public Integer getRowNo() {
			return 1;
		}

		@Override
		public Integer getSeatNo() {
			return 1;
		}

		@Override
		public Integer getTemplateColNo() {
			return 1;
		}

		@Override
		public SeatZone getSeatZone() {
			return SeatZone.LOW;
		}
	}
}