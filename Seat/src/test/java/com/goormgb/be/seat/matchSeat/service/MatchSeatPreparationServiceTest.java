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

	private MatchSeatPreparationService matchSeatPreparationService;

	@BeforeEach
	void setUp() {
		// 테스트 기준 시각: KST 2026-03-17 00:00
		Clock clock = Clock.fixed(
			Instant.parse("2026-03-16T15:00:00Z"),
			ZoneId.of("Asia/Seoul")
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
	void 생성_대상_경기가_없으면_종료한다() {
		// given
		when(matchRepository.findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
			eq(SaleStatus.UPCOMING),
			any(Instant.class),
			any(Instant.class)
		)).thenReturn(List.of());

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		verify(seatRepository, never()).findAllSeatTemplates();
		verify(transactionalService, never()).prepareSingleMatchSeats(any(), any());
	}

	@Test
	@DisplayName("오늘 KST 기준 7일 뒤 날짜 범위의 경기만 조회한다")
	void 오늘_KST_기준_7일_뒤_날짜_범위의_경기만_조회한다() {
		// given
		Match targetMatch = mock(Match.class);
		when(targetMatch.getId()).thenReturn(1L);

		SeatTemplateProjection template = mock(SeatTemplateProjection.class);
		when(seatRepository.findAllSeatTemplates()).thenReturn(List.of(template));

		when(matchRepository.findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
			eq(SaleStatus.UPCOMING),
			any(Instant.class),
			any(Instant.class)
		)).thenReturn(List.of(targetMatch));

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		Instant expectedStart = Instant.parse("2026-03-23T15:00:00Z"); // KST 2026-03-24 00:00
		Instant expectedEnd = Instant.parse("2026-03-24T15:00:00Z");   // KST 2026-03-25 00:00

		verify(matchRepository).findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
			eq(SaleStatus.UPCOMING),
			eq(expectedStart),
			eq(expectedEnd)
		);
		verify(transactionalService, times(1)).prepareSingleMatchSeats(eq(1L), any());
	}

	@Test
	@DisplayName("좌석 템플릿이 없으면 종료한다")
	void 좌석_템플릿이_없으면_종료한다() {
		// given
		Match match = mock(Match.class);

		when(matchRepository.findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
			eq(SaleStatus.UPCOMING),
			any(Instant.class),
			any(Instant.class)
		)).thenReturn(List.of(match));

		when(seatRepository.findAllSeatTemplates()).thenReturn(List.of());

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		verify(transactionalService, never()).prepareSingleMatchSeats(any(), any());
	}

	@Test
	@DisplayName("한 경기 생성 실패해도 다음 경기는 계속 처리한다")
	void 한_경기_생성에_실패해도_다음_경기는_계속_처리한다() {
		// given
		Match match1 = mock(Match.class);
		Match match2 = mock(Match.class);

		when(match1.getId()).thenReturn(1L);
		when(match2.getId()).thenReturn(2L);

		when(matchRepository.findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
			eq(SaleStatus.UPCOMING),
			any(Instant.class),
			any(Instant.class)
		)).thenReturn(List.of(match1, match2));

		SeatTemplateProjection template = mock(SeatTemplateProjection.class);
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

	@Test
	@DisplayName("대상 경기이고 템플릿이 있으면 생성에 성공한다")
	void 대상_경기이고_템플릿이_있으면_생성에_성공한다() {
		// given
		Match match = mock(Match.class);
		when(match.getId()).thenReturn(100L);

		when(matchRepository.findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
			eq(SaleStatus.UPCOMING),
			any(Instant.class),
			any(Instant.class)
		)).thenReturn(List.of(match));

		SeatTemplateProjection template = mock(SeatTemplateProjection.class);
		when(seatRepository.findAllSeatTemplates()).thenReturn(List.of(template));
		when(transactionalService.prepareSingleMatchSeats(eq(100L), any())).thenReturn(true);

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		verify(transactionalService, times(1)).prepareSingleMatchSeats(eq(100L), any());
	}
}