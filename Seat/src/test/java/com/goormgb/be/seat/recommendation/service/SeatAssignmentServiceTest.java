package com.goormgb.be.seat.recommendation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

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
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.fixture.BlockFixture;
import com.goormgb.be.seat.recommendation.dto.response.SeatAssignmentResponse;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;
import com.goormgb.be.seat.redis.SeatSession;

@ExtendWith(MockitoExtension.class)
class SeatAssignmentServiceTest {

	@Mock
	private SeatPreferenceRedisRepository seatPreferenceRedisRepository;
	@Mock
	private BlockRepository blockRepository;
	@Mock
	private SeatBlockLock seatBlockLock;
	@Mock
	private SeatAssignmentTransactionalService seatAssignmentTransactionalService;

	@InjectMocks
	private SeatAssignmentService seatAssignmentService;

	private void setupCommon() {
		SeatSession session = new SeatSession(1L, 1L, true, 3, List.of(1L));
		given(seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(1L, 1L)).willReturn(session);
		given(blockRepository.findByIdWithSectionOrThrow(1L)).willReturn(BlockFixture.cpBlock());
		given(seatBlockLock.tryLock(1L, 1L)).willReturn(true);
	}

	@Test
	@DisplayName("정상 요청 시 락 획득 후 트랜잭션 서비스를 호출하고 락을 해제한다")
	void 정상_요청_락_트랜잭션_순서() {
		// given
		setupCommon();
		Block block = BlockFixture.cpBlock();
		SeatAssignmentResponse expectedResponse = SeatAssignmentResponse.of(
			1L, block, List.of(), Instant.parse("2026-04-15T10:05:00Z"), false);
		given(seatAssignmentTransactionalService.assignAndHold(eq(1L), eq(1L), eq(1L), any(Block.class), eq(3), eq(false)))
			.willReturn(expectedResponse);

		// when
		SeatAssignmentResponse response = seatAssignmentService.assignAndHoldSeats(1L, 1L, 1L, false);

		// then
		assertThat(response).isNotNull();
		then(seatAssignmentTransactionalService).should().assignAndHold(eq(1L), eq(1L), eq(1L), any(Block.class), eq(3), eq(false));
		then(seatBlockLock).should().unlock(1L, 1L);
	}

	@Test
	@DisplayName("락 획득 실패 시 예외가 발생한다")
	void 락_획득_실패_예외() {
		// given
		SeatSession session = new SeatSession(1L, 1L, true, 3, List.of(1L));
		given(seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(1L, 1L)).willReturn(session);
		given(blockRepository.findByIdWithSectionOrThrow(1L)).willReturn(BlockFixture.cpBlock());
		given(seatBlockLock.tryLock(1L, 1L)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> seatAssignmentService.assignAndHoldSeats(1L, 1L, 1L, false))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SEAT_LOCK_ACQUISITION_FAILED);
	}

	@Test
	@DisplayName("트랜잭션 서비스에서 예외 발생 시에도 락이 해제된다")
	void 트랜잭션_예외시_락_해제() {
		// given
		setupCommon();
		given(seatAssignmentTransactionalService.assignAndHold(eq(1L), eq(1L), eq(1L), any(), eq(3), eq(false)))
			.willThrow(new CustomException(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE));

		// when & then
		assertThatThrownBy(() -> seatAssignmentService.assignAndHoldSeats(1L, 1L, 1L, false))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE);

		then(seatBlockLock).should().unlock(1L, 1L);
	}
}
