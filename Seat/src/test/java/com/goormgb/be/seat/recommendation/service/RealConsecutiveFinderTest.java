package com.goormgb.be.seat.recommendation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.recommendation.dto.internal.SeatGroup;
import com.goormgb.be.seat.seat.enums.SeatZone;

@ExtendWith(MockitoExtension.class)
class RealConsecutiveFinderTest {

	@Mock
	private MatchSeatRepository matchSeatRepository;

	@Spy
	private AisleDistanceCalculator aisleDistanceCalculator = new AisleDistanceCalculator();

	@InjectMocks
	private RealConsecutiveFinder realConsecutiveFinder;

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
	@DisplayName("앞열 좌석이 우선 선택된다")
	void 앞열_우선_선택() {
		// given: row1과 row2 모두 연석 가능
		List<MatchSeat> seats = new ArrayList<>();
		for (int col = 1; col <= 14; col++) {
			seats.add(createSeat(1, col));
			seats.add(createSeat(2, col));
		}
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		Optional<SeatGroup> result = realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3);

		// then
		assertThat(result).isPresent();
		assertThat(result.get().rowNo()).isEqualTo(1);
	}

	@Test
	@DisplayName("같은 열에서 통로에 가까운 좌석이 우선 선택된다")
	void 통로_가까운_좌석_우선() {
		// given: row1에 모든 좌석 AVAILABLE
		List<MatchSeat> seats = new ArrayList<>();
		for (int col = 1; col <= 14; col++) {
			seats.add(createSeat(1, col));
		}
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		Optional<SeatGroup> result = realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3);

		// then: 왼쪽 통로에 가까운 [1,2,3]이 선택됨
		assertThat(result).isPresent();
		assertThat(result.get().startCol()).isEqualTo(1);
		assertThat(result.get().aisleDistance()).isEqualTo(0);
	}

	@Test
	@DisplayName("연석 불가능하면 빈 결과를 반환한다")
	void 연석_불가_빈_결과() {
		// given: 모든 좌석이 떨어져 있음
		List<MatchSeat> seats = List.of(
			createSeat(1, 1),
			createSeat(1, 3),
			createSeat(1, 5)
		);
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		Optional<SeatGroup> result = realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 2);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("좌석이 비어있으면 빈 결과를 반환한다")
	void 좌석_없음_빈_결과() {
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(List.of());

		Optional<SeatGroup> result = realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("중간에 빠진 좌석이 있으면 연속 구간 내에서 탐색한다")
	void 중간_빠진_좌석_연속구간() {
		// given: [1,2,3, _, 5,6,7,8,9]
		List<MatchSeat> seats = new ArrayList<>();
		for (int col = 1; col <= 3; col++) {
			seats.add(createSeat(1, col));
		}
		for (int col = 5; col <= 9; col++) {
			seats.add(createSeat(1, col));
		}
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when: 4연석 요청
		Optional<SeatGroup> result = realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 4);

		// then: [5,6,7,8] 선택 (3칸 구간은 4연석 불가)
		assertThat(result).isPresent();
		assertThat(result.get().startCol()).isEqualTo(5);
		assertThat(result.get().seats()).hasSize(4);
	}

	@Test
	@DisplayName("정확히 필요한 수만큼 연속 좌석이 있으면 해당 묶음을 반환한다")
	void 정확히_필요한_수_연석() {
		// given: [5,6,7]
		List<MatchSeat> seats = List.of(
			createSeat(1, 5),
			createSeat(1, 6),
			createSeat(1, 7)
		);
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		Optional<SeatGroup> result = realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3);

		// then
		assertThat(result).isPresent();
		assertThat(result.get().startCol()).isEqualTo(5);
		assertThat(result.get().endCol()).isEqualTo(7);
	}
}
