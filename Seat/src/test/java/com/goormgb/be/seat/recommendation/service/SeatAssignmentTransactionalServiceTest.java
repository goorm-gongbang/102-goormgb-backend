package com.goormgb.be.seat.recommendation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.fixture.BlockFixture;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.recommendation.dto.internal.SeatGroup;
import com.goormgb.be.seat.recommendation.dto.internal.SemiGroup;
import com.goormgb.be.seat.recommendation.dto.response.SeatAssignmentResponse;
import com.goormgb.be.seat.seat.enums.SeatZone;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;

@ExtendWith(MockitoExtension.class)
class SeatAssignmentTransactionalServiceTest {

	@Mock
	private MatchSeatRepository matchSeatRepository;
	@Mock
	private SeatHoldRepository seatHoldRepository;
	@Mock
	private RealConsecutiveFinder realConsecutiveFinder;
	@Mock
	private SemiConsecutiveFinder semiConsecutiveFinder;
	@Mock
	private Clock clock;

	@InjectMocks
	private SeatAssignmentTransactionalService service;

	private static final Instant FIXED_NOW = Instant.parse("2026-04-15T10:00:00Z");

	private MatchSeat createSeat(Long id, int rowNo, int colNo) {
		MatchSeat seat = MatchSeat.builder()
			.matchId(1L)
			.seatId((long)(rowNo * 100 + colNo))
			.areaId(1L)
			.sectionId(1L)
			.blockId(1L)
			.rowNo(rowNo)
			.seatNo(colNo)
			.templateColNo(colNo)
			.seatZone(SeatZone.LOW)
			.saleStatus(MatchSeatSaleStatus.AVAILABLE)
			.build();
		ReflectionTestUtils.setField(seat, "id", id);
		return seat;
	}

	private void setupCommon() {
		given(seatHoldRepository.findAllByUserIdAndMatchId(1L, 1L)).willReturn(List.of());
	}

	@Test
	@DisplayName("진짜 연석이 있으면 조건부 UPDATE로 좌석을 배정한다")
	void 진짜_연석_배정_성공() {
		// given
		setupCommon();
		given(clock.instant()).willReturn(FIXED_NOW);
		List<MatchSeat> seats = List.of(createSeat(101L, 1, 1), createSeat(102L, 1, 2), createSeat(103L, 1, 3));
		SeatGroup seatGroup = new SeatGroup(seats, 1, 1, 3, 0);
		given(realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3)).willReturn(Optional.of(seatGroup));
		given(matchSeatRepository.markBlockedIfAvailable(anyLong())).willReturn(1);

		Block block = BlockFixture.cpBlock();

		// when
		SeatAssignmentResponse response = service.assignAndHold(1L, 1L, 1L, block, 3, false);

		// then
		assertThat(response.assignedSeats()).hasSize(3);
		assertThat(response.semiConsecutive()).isFalse();
		assertThat(response.holdExpiresAt()).isAfter(FIXED_NOW);
		verify(matchSeatRepository).markBlockedIfAvailable(101L);
		verify(matchSeatRepository).markBlockedIfAvailable(102L);
		verify(matchSeatRepository).markBlockedIfAvailable(103L);
		verify(seatHoldRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("진짜 연석이 없고 toggle ON이면 준연석으로 fallback한다")
	void 준연석_fallback_성공() {
		// given
		setupCommon();
		given(clock.instant()).willReturn(FIXED_NOW);
		given(realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3)).willReturn(Optional.empty());

		List<MatchSeat> upperSeats = List.of(createSeat(201L, 1, 1), createSeat(202L, 1, 2));
		List<MatchSeat> lowerSeats = List.of(createSeat(203L, 2, 1));
		SemiGroup semiGroup = new SemiGroup(upperSeats, lowerSeats, 1, 2, 1, 0);
		given(semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 3)).willReturn(Optional.of(semiGroup));
		given(matchSeatRepository.markBlockedIfAvailable(anyLong())).willReturn(1);

		Block block = BlockFixture.cpBlock();

		// when
		SeatAssignmentResponse response = service.assignAndHold(1L, 1L, 1L, block, 3, true);

		// then
		assertThat(response.assignedSeats()).hasSize(3);
		assertThat(response.semiConsecutive()).isTrue();
	}

	@Test
	@DisplayName("진짜 연석이 없고 toggle OFF이면 예외가 발생한다")
	void toggle_OFF_연석없음_예외() {
		// given
		setupCommon();
		given(realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3)).willReturn(Optional.empty());

		Block block = BlockFixture.cpBlock();

		// when & then
		assertThatThrownBy(() -> service.assignAndHold(1L, 1L, 1L, block, 3, false))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE);
	}

	@Test
	@DisplayName("진짜 연석도 준연석도 없으면 예외가 발생한다")
	void 연석_준연석_모두_없음_예외() {
		// given
		setupCommon();
		given(realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3)).willReturn(Optional.empty());
		given(semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 3)).willReturn(Optional.empty());

		Block block = BlockFixture.cpBlock();

		// when & then
		assertThatThrownBy(() -> service.assignAndHold(1L, 1L, 1L, block, 3, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE);
	}

	@Test
	@DisplayName("충돌 발생 시 롤백 후 다른 연석 구간을 재탐색하여 배정한다")
	void 충돌_후_재탐색_성공() {
		// given
		setupCommon();
		given(clock.instant()).willReturn(FIXED_NOW);

		// 1차 시도: 1열 1~3번 → 3번 좌석에서 충돌
		List<MatchSeat> firstAttempt = List.of(createSeat(101L, 1, 1), createSeat(102L, 1, 2), createSeat(103L, 1, 3));
		SeatGroup firstGroup = new SeatGroup(firstAttempt, 1, 1, 3, 0);

		// 2차 시도: 2열 5~7번 → 성공
		List<MatchSeat> secondAttempt = List.of(createSeat(205L, 2, 5), createSeat(206L, 2, 6), createSeat(207L, 2, 7));
		SeatGroup secondGroup = new SeatGroup(secondAttempt, 2, 5, 7, 1);

		given(realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3))
			.willReturn(Optional.of(firstGroup))
			.willReturn(Optional.of(secondGroup));

		// 1차: seat1 성공, seat2 성공, seat3 충돌
		given(matchSeatRepository.markBlockedIfAvailable(101L)).willReturn(1);
		given(matchSeatRepository.markBlockedIfAvailable(102L)).willReturn(1);
		given(matchSeatRepository.markBlockedIfAvailable(103L)).willReturn(0);
		// 2차: 모두 성공
		given(matchSeatRepository.markBlockedIfAvailable(205L)).willReturn(1);
		given(matchSeatRepository.markBlockedIfAvailable(206L)).willReturn(1);
		given(matchSeatRepository.markBlockedIfAvailable(207L)).willReturn(1);

		Block block = BlockFixture.cpBlock();

		// when
		SeatAssignmentResponse response = service.assignAndHold(1L, 1L, 1L, block, 3, false);

		// then
		assertThat(response.assignedSeats()).hasSize(3);
		assertThat(response.semiConsecutive()).isFalse();

		// 1차 시도의 seat1, seat2 일괄 롤백 확인
		verify(matchSeatRepository).markAvailableIfBlockedInBatch(List.of(101L, 102L));

		// finder가 2번 호출됨
		verify(realConsecutiveFinder, times(2)).findBestRealConsecutive(1L, 1L, 3);
		verify(seatHoldRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("진짜 연석 재시도 소진 후 준연석으로 fallback하여 성공한다")
	void 진짜연석_재시도_소진_후_준연석_성공() {
		// given
		setupCommon();
		given(clock.instant()).willReturn(FIXED_NOW);

		// 진짜 연석: 3번 모두 충돌
		List<MatchSeat> realSeats = List.of(createSeat(101L, 1, 1), createSeat(102L, 1, 2));
		SeatGroup realGroup = new SeatGroup(realSeats, 1, 1, 2, 0);
		given(realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 2))
			.willReturn(Optional.of(realGroup));
		given(matchSeatRepository.markBlockedIfAvailable(101L)).willReturn(0);

		// 준연석: 성공
		List<MatchSeat> upperSeats = List.of(createSeat(301L, 3, 1));
		List<MatchSeat> lowerSeats = List.of(createSeat(401L, 4, 1));
		SemiGroup semiGroup = new SemiGroup(upperSeats, lowerSeats, 3, 4, 1, 0);
		given(semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 2))
			.willReturn(Optional.of(semiGroup));
		given(matchSeatRepository.markBlockedIfAvailable(301L)).willReturn(1);
		given(matchSeatRepository.markBlockedIfAvailable(401L)).willReturn(1);

		Block block = BlockFixture.cpBlock();

		// when
		SeatAssignmentResponse response = service.assignAndHold(1L, 1L, 1L, block, 2, true);

		// then
		assertThat(response.assignedSeats()).hasSize(2);
		assertThat(response.semiConsecutive()).isTrue();

		// 진짜 연석 3번 시도
		verify(realConsecutiveFinder, times(3)).findBestRealConsecutive(1L, 1L, 2);
		// 준연석 1번 시도 성공
		verify(semiConsecutiveFinder, times(1)).findBestSemiConsecutive(1L, 1L, 2);
	}

	@Test
	@DisplayName("진짜 연석, 준연석 모두 재시도 소진 시 예외가 발생한다")
	void 모든_재시도_소진_예외() {
		// given
		setupCommon();
		given(clock.instant()).willReturn(FIXED_NOW);

		List<MatchSeat> seats = List.of(createSeat(101L, 1, 1));
		SeatGroup realGroup = new SeatGroup(seats, 1, 1, 1, 0);
		given(realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 1))
			.willReturn(Optional.of(realGroup));
		given(matchSeatRepository.markBlockedIfAvailable(101L)).willReturn(0);

		List<MatchSeat> semiUpper = List.of(createSeat(201L, 2, 1));
		SemiGroup semiGroup = new SemiGroup(semiUpper, List.of(), 2, 3, 0, 0);
		given(semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 1))
			.willReturn(Optional.of(semiGroup));
		given(matchSeatRepository.markBlockedIfAvailable(201L)).willReturn(0);

		Block block = BlockFixture.cpBlock();

		// when & then
		assertThatThrownBy(() -> service.assignAndHold(1L, 1L, 1L, block, 1, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE);

		verify(realConsecutiveFinder, times(3)).findBestRealConsecutive(1L, 1L, 1);
		verify(semiConsecutiveFinder, times(3)).findBestSemiConsecutive(1L, 1L, 1);
	}
}
