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
import com.goormgb.be.seat.recommendation.dto.internal.SemiGroup;
import com.goormgb.be.seat.seat.enums.SeatZone;

@ExtendWith(MockitoExtension.class)
class SemiConsecutiveFinderTest {

	@Mock
	private MatchSeatRepository matchSeatRepository;

	@Spy
	private AisleDistanceCalculator aisleDistanceCalculator = new AisleDistanceCalculator();

	@InjectMocks
	private SemiConsecutiveFinder semiConsecutiveFinder;

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
	@DisplayName("인접 2개 열에서 수평 겹침이 있는 준연석을 찾는다")
	void 준연석_정상_탐색() {
		// given: row1에 [1,2,3], row2에 [2,3,4]
		List<MatchSeat> seats = new ArrayList<>();
		for (int col = 1; col <= 3; col++) {
			seats.add(createSeat(1, col));
		}
		for (int col = 2; col <= 4; col++) {
			seats.add(createSeat(2, col));
		}
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when: 4석 요청
		Optional<SemiGroup> result = semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 4);

		// then
		assertThat(result).isPresent();
		SemiGroup group = result.get();
		assertThat(group.upperRowNo()).isEqualTo(1);
		assertThat(group.lowerRowNo()).isEqualTo(2);
		int totalSeats = group.upperSeats().size() + group.lowerSeats().size();
		assertThat(totalSeats).isEqualTo(4);
		assertThat(group.overlapCount()).isGreaterThan(0);
	}

	@Test
	@DisplayName("수평 겹침이 없는 조합은 제외된다")
	void 겹침없는_조합_제외() {
		// given: row1에 [1,2], row2에 [10,11]
		List<MatchSeat> seats = List.of(
			createSeat(1, 1),
			createSeat(1, 2),
			createSeat(2, 10),
			createSeat(2, 11)
		);
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when: 4석 요청
		Optional<SemiGroup> result = semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 4);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("비인접 열은 준연석 후보에서 제외된다")
	void 비인접_열_제외() {
		// given: row1에 [1,2,3], row3에 [1,2,3] (row2 없음)
		List<MatchSeat> seats = new ArrayList<>();
		for (int col = 1; col <= 3; col++) {
			seats.add(createSeat(1, col));
		}
		for (int col = 1; col <= 3; col++) {
			seats.add(createSeat(3, col));
		}
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		Optional<SemiGroup> result = semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 4);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("좌석이 부족하면 빈 결과를 반환한다")
	void 좌석_부족_빈_결과() {
		List<MatchSeat> seats = List.of(
			createSeat(1, 1),
			createSeat(2, 1)
		);
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		Optional<SemiGroup> result = semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 4);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("앞열 합이 작은 준연석이 우선 선택된다")
	void 앞열_합_우선() {
		// given: row1+row2, row8+row9 모두 가능
		List<MatchSeat> seats = new ArrayList<>();
		for (int col = 1; col <= 5; col++) {
			seats.add(createSeat(1, col));
			seats.add(createSeat(2, col));
			seats.add(createSeat(8, col));
			seats.add(createSeat(9, col));
		}
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when
		Optional<SemiGroup> result = semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 4);

		// then: row1+row2 (합=3) 우선
		assertThat(result).isPresent();
		assertThat(result.get().rowSum()).isEqualTo(3);
	}

	@Test
	@DisplayName("겹침이 많은 준연석이 우선 선택된다")
	void 겹침_많은_우선() {
		// given: row1에 [1,2,3,4,5], row2에 [1,2,3,4,5]
		List<MatchSeat> seats = new ArrayList<>();
		for (int col = 1; col <= 5; col++) {
			seats.add(createSeat(1, col));
			seats.add(createSeat(2, col));
		}
		given(matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(1L, 1L)).willReturn(seats);

		// when: 4석 요청
		Optional<SemiGroup> result = semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 4);

		// then
		assertThat(result).isPresent();
		assertThat(result.get().overlapCount()).isGreaterThanOrEqualTo(1);
	}
}
