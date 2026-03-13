package com.goormgb.be.seat.matchSeat.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.seat.seat.dto.SeatTemplateProjection;
import com.goormgb.be.seat.seat.repository.SeatRepository;

@ExtendWith(MockitoExtension.class)
class MatchSeatPreparationServiceTest {

	@Mock
	private MatchRepository matchRepository;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private MatchSeatPreparationTransactionalService transactionalService;

	private Clock clock;

	@InjectMocks
	private MatchSeatPreparationService matchSeatPreparationService;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(
			Instant.parse("2026-03-13T00:00:00Z"),
			ZoneId.of("UTC")
		);

		matchSeatPreparationService = new MatchSeatPreparationService(
			matchRepository,
			seatRepository,
			clock,
			transactionalService
		);
	}

	@Test
	@DisplayName("생성 대상 경기가 없으면 종료한다")
	void prepareMatchSeats_noMatches() {
		// given
		when(matchRepository.findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
			eq(SaleStatus.UPCOMING), any(), any()
		)).thenReturn(List.of());

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		verify(seatRepository, never()).findAllSeatTemplates();
		verify(transactionalService, never()).prepareSingleMatchSeats(any(), any());
	}

	@Test
	@DisplayName("좌석 템플릿이 없으면 종료한다")
	void prepareMatchSeats_noTemplates() {
		// given
		Match match = org.mockito.Mockito.mock(Match.class);
		when(matchRepository.findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
			eq(SaleStatus.UPCOMING), any(), any()
		)).thenReturn(List.of(match));

		when(seatRepository.findAllSeatTemplates()).thenReturn(List.of());

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		verify(transactionalService, never()).prepareSingleMatchSeats(any(), any());
	}

	@Test
	@DisplayName("대상 경기마다 match seat 생성을 시도한다")
	void prepareMatchSeats_callsTransactionalServiceForEachMatch() {
		// given
		Match match1 = org.mockito.Mockito.mock(Match.class);
		Match match2 = org.mockito.Mockito.mock(Match.class);

		when(match1.getId()).thenReturn(1L);
		when(match2.getId()).thenReturn(2L);

		when(matchRepository.findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
			eq(SaleStatus.UPCOMING), any(), any()
		)).thenReturn(List.of(match1, match2));

		SeatTemplateProjection template = org.mockito.Mockito.mock(SeatTemplateProjection.class);
		when(seatRepository.findAllSeatTemplates()).thenReturn(List.of(template));

		when(transactionalService.prepareSingleMatchSeats(eq(1L), any())).thenReturn(true);
		when(transactionalService.prepareSingleMatchSeats(eq(2L), any())).thenReturn(true);

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		verify(transactionalService, times(1)).prepareSingleMatchSeats(eq(1L), any());
		verify(transactionalService, times(1)).prepareSingleMatchSeats(eq(2L), any());
	}

	@Test
	@DisplayName("한 경기 생성 실패해도 다음 경기는 계속 처리한다")
	void prepareMatchSeats_continueWhenOneMatchFails() {
		// given
		Match match1 = org.mockito.Mockito.mock(Match.class);
		Match match2 = org.mockito.Mockito.mock(Match.class);

		when(match1.getId()).thenReturn(1L);
		when(match2.getId()).thenReturn(2L);

		when(matchRepository.findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
			eq(SaleStatus.UPCOMING), any(), any()
		)).thenReturn(List.of(match1, match2));

		SeatTemplateProjection template = org.mockito.Mockito.mock(SeatTemplateProjection.class);
		when(seatRepository.findAllSeatTemplates()).thenReturn(List.of(template));

		doThrow(new RuntimeException("DB 오류"))
			.when(transactionalService).prepareSingleMatchSeats(eq(1L), any());

		when(transactionalService.prepareSingleMatchSeats(eq(2L), any())).thenReturn(true);

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		verify(transactionalService, times(1)).prepareSingleMatchSeats(eq(1L), any());
		verify(transactionalService, times(1)).prepareSingleMatchSeats(eq(2L), any());
	}
}
