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

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.fixture.BlockFixture;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.recommendation.dto.internal.SemiGroup;
import com.goormgb.be.seat.recommendation.dto.internal.SeatGroup;
import com.goormgb.be.seat.recommendation.dto.response.SeatAssignmentResponse;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;
import com.goormgb.be.seat.redis.SeatSession;
import com.goormgb.be.seat.seat.enums.SeatZone;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;

@ExtendWith(MockitoExtension.class)
class SeatAssignmentServiceTest {

	@Mock
	private SeatPreferenceRedisRepository seatPreferenceRedisRepository;
	@Mock
	private BlockRepository blockRepository;
	@Mock
	private MatchSeatRepository matchSeatRepository;
	@Mock
	private SeatHoldRepository seatHoldRepository;
	@Mock
	private RealConsecutiveFinder realConsecutiveFinder;
	@Mock
	private SemiConsecutiveFinder semiConsecutiveFinder;
	@Mock
	private SeatBlockLock seatBlockLock;
	@Mock
	private Clock clock;

	@InjectMocks
	private SeatAssignmentService seatAssignmentService;

	private static final Instant FIXED_NOW = Instant.parse("2026-04-15T10:00:00Z");

	private MatchSeat createSeat(int rowNo, int colNo) {
		return MatchSeat.builder()
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
	}

	private void setupCommon() {
		SeatSession session = new SeatSession(1L, 1L, true, 3, List.of(1L));
		given(seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(1L, 1L)).willReturn(session);

		Block block = BlockFixture.cpBlock();
		given(blockRepository.findByIdWithSectionOrThrow(1L)).willReturn(block);
		given(seatBlockLock.tryLock(1L)).willReturn(true);
		given(seatHoldRepository.findAllByUserIdAndMatchId(1L, 1L)).willReturn(List.of());
	}

	private void setupClockForHold() {
		given(clock.instant()).willReturn(FIXED_NOW);
	}

	@Test
	@DisplayName("진짜 연석이 있으면 해당 좌석을 배정한다")
	void 진짜_연석_배정_성공() {
		// given
		setupCommon();
		setupClockForHold();
		List<MatchSeat> seats = List.of(createSeat(1, 1), createSeat(1, 2), createSeat(1, 3));
		SeatGroup seatGroup = new SeatGroup(seats, 1, 1, 3, 0);
		given(realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3)).willReturn(Optional.of(seatGroup));

		// when
		SeatAssignmentResponse response = seatAssignmentService.assignAndHoldSeats(1L, 1L, 1L, false);

		// then
		assertThat(response.assignedSeats()).hasSize(3);
		assertThat(response.semiConsecutive()).isFalse();
		assertThat(response.holdExpiresAt()).isAfter(FIXED_NOW);
		verify(seatHoldRepository).saveAll(anyList());
		verify(seatBlockLock).unlock(1L);
	}

	@Test
	@DisplayName("진짜 연석이 없고 toggle ON이면 준연석으로 fallback한다")
	void 준연석_fallback_성공() {
		// given
		setupCommon();
		setupClockForHold();
		given(realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3)).willReturn(Optional.empty());

		List<MatchSeat> upperSeats = List.of(createSeat(1, 1), createSeat(1, 2));
		List<MatchSeat> lowerSeats = List.of(createSeat(2, 1));
		SemiGroup semiGroup = new SemiGroup(upperSeats, lowerSeats, 1, 2, 1, 0);
		given(semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 3)).willReturn(Optional.of(semiGroup));

		// when
		SeatAssignmentResponse response = seatAssignmentService.assignAndHoldSeats(1L, 1L, 1L, true);

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

		// when & then
		assertThatThrownBy(() -> seatAssignmentService.assignAndHoldSeats(1L, 1L, 1L, false))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE);
		verify(seatBlockLock).unlock(1L);
	}

	@Test
	@DisplayName("락 획득 실패 시 예외가 발생한다")
	void 락_획득_실패_예외() {
		// given
		SeatSession session = new SeatSession(1L, 1L, true, 3, List.of(1L));
		given(seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(1L, 1L)).willReturn(session);
		given(blockRepository.findByIdWithSectionOrThrow(1L)).willReturn(BlockFixture.cpBlock());
		given(seatBlockLock.tryLock(1L)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> seatAssignmentService.assignAndHoldSeats(1L, 1L, 1L, false))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SEAT_LOCK_ACQUISITION_FAILED);
	}

	@Test
	@DisplayName("진짜 연석도 준연석도 없으면 예외가 발생한다")
	void 연석_준연석_모두_없음_예외() {
		// given
		setupCommon();
		given(realConsecutiveFinder.findBestRealConsecutive(1L, 1L, 3)).willReturn(Optional.empty());
		given(semiConsecutiveFinder.findBestSemiConsecutive(1L, 1L, 3)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> seatAssignmentService.assignAndHoldSeats(1L, 1L, 1L, true))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE);
	}
}
