package com.goormgb.be.seat.common.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.common.dto.response.SeatHoldCreateResponse;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.metrics.SeatMetricsService;
import com.goormgb.be.seat.seat.enums.SeatZone;
import com.goormgb.be.seat.seatHold.entity.SeatHold;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;

@ExtendWith(MockitoExtension.class)
class SeatHoldTransactionalServiceTest {

	private static final Long USER_ID = 7L;
	private static final Long MATCH_ID = 10L;
	private static final Instant NOW = Instant.parse("2026-04-15T10:00:00Z");

	@Mock
	private SeatMetricsService seatMetricsService;
	@Mock
	private MatchSeatRepository matchSeatRepository;
	@Mock
	private SeatHoldRepository seatHoldRepository;
	@Mock
	private Clock clock;

	@InjectMocks
	private SeatHoldTransactionalService seatHoldTransactionalService;

	private MatchSeat matchSeat(Long seatId, MatchSeatSaleStatus status) {
		return MatchSeat.builder()
			.matchId(MATCH_ID)
			.seatId(seatId)
			.areaId(1L)
			.sectionId(1L)
			.blockId(1L)
			.rowNo(1)
			.seatNo(seatId.intValue())
			.templateColNo(seatId.intValue())
			.seatZone(SeatZone.LOW)
			.saleStatus(status)
			.build();
	}

	private SeatHold seatHold(Long matchSeatId, Long seatId, Long userId, Instant expiresAt) {
		return SeatHold.builder()
			.matchSeatId(matchSeatId)
			.matchId(MATCH_ID)
			.seatId(seatId)
			.userId(userId)
			.expiresAt(expiresAt)
			.build();
	}

	@Test
	@DisplayName("동일 좌석 재요청이면 hold 만료시간을 연장한다")
	void 동일_좌석_재요청_만료_연장() {
		// given
		given(clock.instant()).willReturn(NOW);

		given(matchSeatRepository.findAllByMatchIdAndSeatIdIn(MATCH_ID, List.of(206313L, 206314L)))
			.willReturn(List.of(matchSeat(206313L, MatchSeatSaleStatus.AVAILABLE),
				matchSeat(206314L, MatchSeatSaleStatus.AVAILABLE)));
		given(
			seatHoldRepository.findAllByMatchIdAndSeatIdInAndExpiresAtAfter(eq(MATCH_ID), eq(List.of(206313L, 206314L)),
				any()))
			.willReturn(List.of(
				seatHold(1L, 206313L, USER_ID, NOW.plusSeconds(60)),
				seatHold(2L, 206314L, USER_ID, NOW.plusSeconds(60))
			));
		given(seatHoldRepository.findAllByUserIdAndMatchIdAndExpiresAtAfter(eq(USER_ID), eq(MATCH_ID), any()))
			.willReturn(List.of(
				seatHold(1L, 206313L, USER_ID, NOW.plusSeconds(60)),
				seatHold(2L, 206314L, USER_ID, NOW.plusSeconds(60))
			));

		// when
		SeatHoldCreateResponse response = seatHoldTransactionalService.createOrRefreshHold(USER_ID, MATCH_ID,
			List.of(206313L, 206314L));

		// then
		assertThat(response.matchId()).isEqualTo(MATCH_ID);
		assertThat(response.seatCount()).isEqualTo(2);
		assertThat(response.holdExpiresAt()).isEqualTo(NOW.plusSeconds(300));
		verify(seatHoldRepository, never()).saveAll(anyList());
		verify(seatHoldRepository, never()).deleteAllByMatchSeatIdIn(anyList());
	}

	@Test
	@DisplayName("기존 hold와 다른 좌석 요청이면 기존 hold를 해제하고 신규 hold를 생성한다")
	void 다른_좌석_요청시_교체_성공() {
		// given
		given(clock.instant()).willReturn(NOW);

		given(matchSeatRepository.findAllByMatchIdAndSeatIdIn(MATCH_ID, List.of(206313L, 206314L)))
			.willReturn(List.of(matchSeat(206313L, MatchSeatSaleStatus.AVAILABLE),
				matchSeat(206314L, MatchSeatSaleStatus.AVAILABLE)));
		given(
			seatHoldRepository.findAllByMatchIdAndSeatIdInAndExpiresAtAfter(eq(MATCH_ID), eq(List.of(206313L, 206314L)),
				any()))
			.willReturn(List.of());
		given(seatHoldRepository.findAllByUserIdAndMatchIdAndExpiresAtAfter(eq(USER_ID), eq(MATCH_ID), any()))
			.willReturn(List.of(
				seatHold(11L, 205000L, USER_ID, NOW.plusSeconds(60)),
				seatHold(12L, 205001L, USER_ID, NOW.plusSeconds(60))
			));
		given(matchSeatRepository.findAllById(List.of(11L, 12L)))
			.willReturn(List.of(matchSeat(205000L, MatchSeatSaleStatus.BLOCKED),
				matchSeat(205001L, MatchSeatSaleStatus.BLOCKED)));

		// when
		SeatHoldCreateResponse response = seatHoldTransactionalService.createOrRefreshHold(USER_ID, MATCH_ID,
			List.of(206313L, 206314L));

		// then
		assertThat(response.seatIds()).containsExactly(206313L, 206314L);
		verify(seatHoldRepository).deleteAllByMatchSeatIdIn(List.of(11L, 12L));
		verify(seatHoldRepository).flush();
		verify(seatHoldRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("타 사용자 활성 hold가 있으면 SEAT_ALREADY_HELD_BY_OTHER 예외가 발생한다")
	void 타사용자_선점_충돌_예외() {
		// given
		given(clock.instant()).willReturn(NOW);

		given(matchSeatRepository.findAllByMatchIdAndSeatIdIn(MATCH_ID, List.of(206313L, 206314L)))
			.willReturn(List.of(matchSeat(206313L, MatchSeatSaleStatus.AVAILABLE),
				matchSeat(206314L, MatchSeatSaleStatus.AVAILABLE)));
		given(
			seatHoldRepository.findAllByMatchIdAndSeatIdInAndExpiresAtAfter(eq(MATCH_ID), eq(List.of(206313L, 206314L)),
				any()))
			.willReturn(List.of(seatHold(1L, 206313L, 999L, NOW.plusSeconds(60))));

		// when & then
		assertThatThrownBy(
			() -> seatHoldTransactionalService.createOrRefreshHold(USER_ID, MATCH_ID, List.of(206313L, 206314L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SEAT_ALREADY_HELD_BY_OTHER);

		verify(seatHoldRepository, never()).saveAll(anyList());
	}

	@Test
	@DisplayName("이미 판매된 좌석이 포함되면 SEAT_ALREADY_SOLD 예외가 발생한다")
	void 판매완료_좌석_예외() {
		// given
		given(clock.instant()).willReturn(NOW);

		given(matchSeatRepository.findAllByMatchIdAndSeatIdIn(MATCH_ID, List.of(206313L, 206314L)))
			.willReturn(List.of(matchSeat(206313L, MatchSeatSaleStatus.SOLD),
				matchSeat(206314L, MatchSeatSaleStatus.AVAILABLE)));

		// when & then
		assertThatThrownBy(
			() -> seatHoldTransactionalService.createOrRefreshHold(USER_ID, MATCH_ID, List.of(206313L, 206314L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SEAT_ALREADY_SOLD);
	}

	@Test
	@DisplayName("요청한 좌석이 DB에 없으면 MATCH_SEAT_NOT_FOUND 예외가 발생한다")
	void 좌석_미존재_예외() {
		// given
		given(clock.instant()).willReturn(NOW);

		given(matchSeatRepository.findAllByMatchIdAndSeatIdIn(MATCH_ID, List.of(206313L, 206314L)))
			.willReturn(List.of(matchSeat(206313L, MatchSeatSaleStatus.AVAILABLE)));

		// when & then
		assertThatThrownBy(
			() -> seatHoldTransactionalService.createOrRefreshHold(USER_ID, MATCH_ID, List.of(206313L, 206314L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.MATCH_SEAT_NOT_FOUND);
	}
}
