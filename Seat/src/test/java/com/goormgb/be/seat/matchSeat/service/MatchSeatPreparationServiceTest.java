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
		when(matchRepository.findBySaleStatus(eq(SaleStatus.UPCOMING))).thenReturn(List.of());

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		verify(seatRepository, never()).findAllSeatTemplates();
		verify(transactionalService, never()).prepareSingleMatchSeats(any(), any());
	}

	@Test
	@DisplayName("오늘 KST 기준 7일 뒤 경기만 생성 대상에 포함한다")
	void 오늘_KST_기준_7일_뒤_경기만_생성_대상에_포함한다() {
		// given
		// todayKst = 2026-03-17
		// 생성 대상 경기일 = 2026-03-24 (KST)
		Match targetMatch = mock(Match.class);
		Match nonTargetMatch = mock(Match.class);

		// 생성 대상 경기
		when(targetMatch.getId()).thenReturn(1L);
		when(targetMatch.getMatchAt()).thenReturn(Instant.parse("2026-03-24T03:00:00Z")); // KST 2026-03-24 12:00

		// 생성 대상이 아닌 경기
		when(nonTargetMatch.getMatchAt()).thenReturn(Instant.parse("2026-03-25T03:00:00Z")); // KST 2026-03-25 12:00

		when(matchRepository.findBySaleStatus(eq(SaleStatus.UPCOMING)))
			.thenReturn(List.of(targetMatch, nonTargetMatch));

		SeatTemplateProjection template = mock(SeatTemplateProjection.class);
		when(seatRepository.findAllSeatTemplates()).thenReturn(List.of(template));

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		verify(transactionalService, times(1)).prepareSingleMatchSeats(eq(1L), any());
		verify(transactionalService, never()).prepareSingleMatchSeats(eq(2L), any());
	}

	@Test
	@DisplayName("좌석 템플릿이 없으면 종료한다")
	void 좌석_템플릿이_없으면_종료한다() {
		// given
		// 생성 대상 경기는 있지만 좌석 템플릿이 없어서 실제 생성은 진행되지 않아야 한다.
		Match match = mock(Match.class);
		when(match.getMatchAt()).thenReturn(Instant.parse("2026-03-24T03:00:00Z")); // KST 2026-03-24 12:00

		when(matchRepository.findBySaleStatus(eq(SaleStatus.UPCOMING))).thenReturn(List.of(match));
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
		// 두 경기 모두 생성 대상 날짜(KST 2026-03-24)로 설정
		Match match1 = mock(Match.class);
		Match match2 = mock(Match.class);

		when(match1.getId()).thenReturn(1L);
		when(match2.getId()).thenReturn(2L);

		when(match1.getMatchAt()).thenReturn(Instant.parse("2026-03-24T03:00:00Z")); // KST 12:00
		when(match2.getMatchAt()).thenReturn(Instant.parse("2026-03-24T09:00:00Z")); // KST 18:00

		when(matchRepository.findBySaleStatus(eq(SaleStatus.UPCOMING)))
			.thenReturn(List.of(match1, match2));

		SeatTemplateProjection template = mock(SeatTemplateProjection.class);
		when(seatRepository.findAllSeatTemplates()).thenReturn(List.of(template));

		// 첫 번째 경기 생성은 실패
		doThrow(new RuntimeException("DB 오류"))
			.when(transactionalService).prepareSingleMatchSeats(eq(1L), any());

		// 두 번째 경기는 정상 생성
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
		// KST 기준 2026-03-24 경기이므로 생성 대상
		Match match = mock(Match.class);
		when(match.getId()).thenReturn(100L);
		when(match.getMatchAt()).thenReturn(Instant.parse("2026-03-24T03:00:00Z")); // KST 12:00

		when(matchRepository.findBySaleStatus(eq(SaleStatus.UPCOMING))).thenReturn(List.of(match));

		SeatTemplateProjection template = mock(SeatTemplateProjection.class);
		when(seatRepository.findAllSeatTemplates()).thenReturn(List.of(template));
		when(transactionalService.prepareSingleMatchSeats(eq(100L), any())).thenReturn(true);

		// when
		matchSeatPreparationService.prepareMatchSeats();

		// then
		verify(transactionalService, times(1)).prepareSingleMatchSeats(eq(100L), any());
	}
}