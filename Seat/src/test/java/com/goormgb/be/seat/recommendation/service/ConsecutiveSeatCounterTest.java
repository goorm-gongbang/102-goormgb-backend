package com.goormgb.be.seat.recommendation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.seat.enums.SeatZone;

@ExtendWith(MockitoExtension.class)
class ConsecutiveSeatCounterTest {

	@Mock
	private MatchSeatRepository matchSeatRepository;

	@InjectMocks
	private ConsecutiveSeatCounter consecutiveSeatCounter;

	private MatchSeat createSeat(int rowNo, int templateColNo) {
		return MatchSeat.builder()
			.matchId(1L)
			.seatId((long)(rowNo * 100 + templateColNo))
			.areaId(1L)
			.sectionId(1L)
			.blockId(1L)
			.rowNo(rowNo)
			.seatNo(templateColNo)
			.templateColNo(templateColNo)
			.seatZone(SeatZone.LOW)
			.saleStatus(MatchSeatSaleStatus.AVAILABLE)
			.build();
	}

	@Test
	@DisplayName("14칸 연속 row에서 5연석 가능 개수는 10개이다")
	void 전체_연속_14칸에서_5연석() {
		// given
		List<MatchSeat> seats = new java.util.ArrayList<>();
		for (int col = 1; col <= 14; col++) {
			seats.add(createSeat(1, col));
		}
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		int count = consecutiveSeatCounter.countRealConsecutiveSeats(1L, 1L, 5);

		// then
		assertThat(count).isEqualTo(10); // 14 - 5 + 1 = 10
	}

	@Test
	@DisplayName("중간에 빠진 좌석이 있으면 연속 구간별로 계산한다")
	void 중간_빠진_좌석_연속구간_분리() {
		// given: [1,2,3,4,5, _, 7,8,9,10,11,12,13,14]
		List<MatchSeat> seats = new java.util.ArrayList<>();
		for (int col = 1; col <= 5; col++) {
			seats.add(createSeat(1, col));
		}
		for (int col = 7; col <= 14; col++) {
			seats.add(createSeat(1, col));
		}
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		int count = consecutiveSeatCounter.countRealConsecutiveSeats(1L, 1L, 5);

		// then
		// [1~5] → 5-5+1 = 1개
		// [7~14] → 8-5+1 = 4개
		// 합계 = 5개
		assertThat(count).isEqualTo(5);
	}

	@Test
	@DisplayName("여러 row에서 연석 개수를 합산한다")
	void 여러_row_합산() {
		// given: row1에 [1~14], row2에 [1~14]
		List<MatchSeat> seats = new java.util.ArrayList<>();
		for (int col = 1; col <= 14; col++) {
			seats.add(createSeat(1, col));
		}
		for (int col = 1; col <= 14; col++) {
			seats.add(createSeat(2, col));
		}
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		int count = consecutiveSeatCounter.countRealConsecutiveSeats(1L, 1L, 5);

		// then
		assertThat(count).isEqualTo(20); // (14-5+1) * 2 = 20
	}

	@Test
	@DisplayName("연석 가능한 좌석이 없으면 0을 반환한다")
	void 연석_불가_0반환() {
		// given: [1, 3, 5, 7] — 모두 떨어져 있음
		List<MatchSeat> seats = List.of(
			createSeat(1, 1),
			createSeat(1, 3),
			createSeat(1, 5),
			createSeat(1, 7)
		);
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		int count = consecutiveSeatCounter.countRealConsecutiveSeats(1L, 1L, 2);

		// then
		assertThat(count).isEqualTo(0);
	}

	@Test
	@DisplayName("좌석이 비어있으면 0을 반환한다")
	void 좌석_없음_0반환() {
		// given
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(List.of());

		// when
		int count = consecutiveSeatCounter.countRealConsecutiveSeats(1L, 1L, 3);

		// then
		assertThat(count).isEqualTo(0);
	}

	@Test
	@DisplayName("1매 요청 시 AVAILABLE 좌석 수만큼 반환한다")
	void 단일석_요청() {
		// given
		List<MatchSeat> seats = List.of(
			createSeat(1, 1),
			createSeat(1, 5),
			createSeat(2, 3)
		);
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		int count = consecutiveSeatCounter.countRealConsecutiveSeats(1L, 1L, 1);

		// then
		assertThat(count).isEqualTo(3);
	}
}
