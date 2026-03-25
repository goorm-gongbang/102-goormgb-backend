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
import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.fixture.BlockFixture;
import com.goormgb.be.seat.booking.model.BookingOptions;
import com.goormgb.be.seat.booking.repository.BookingOptionsRedisRepository;
import com.goormgb.be.seat.recommendation.dto.response.SeatAssignmentResponse;

@ExtendWith(MockitoExtension.class)
class SeatAssignmentServiceTest {

	@Mock
	private BookingOptionsRedisRepository bookingOptionsRedisRepository;
	@Mock
	private BlockRepository blockRepository;
	@Mock
	private SeatBlockLock seatBlockLock;
	@Mock
	private SeatAssignmentTransactionalService seatAssignmentTransactionalService;

	@InjectMocks
	private SeatAssignmentService seatAssignmentService;

	private static final Long MATCH_ID = 1L;
	private static final Long USER_ID = 1L;
	private static final Long BLOCK_NUM = 1L; // CP block

	private Block cpBlock;

	private void setupCommon() {
		cpBlock = BlockFixture.cpBlock();
		ReflectionTestUtils.setField(cpBlock, "id", 99L); // PK와 blockNum이 다름을 검증
		given(bookingOptionsRedisRepository.getByUserIdAndMatchIdOrThrow(USER_ID, MATCH_ID))
			.willReturn(new BookingOptions(USER_ID, MATCH_ID, true, 3, false, Instant.now()));
		given(blockRepository.findByBlockNumWithSectionOrThrow(BLOCK_NUM)).willReturn(cpBlock);
		given(seatBlockLock.tryLock(MATCH_ID, 99L)).willReturn(true);
	}

	@Test
	@DisplayName("정상 요청 시 blockNum으로 Block을 조회하고 PK로 락을 획득한다")
	void 정상_요청_락_트랜잭션_순서() {
		// given
		setupCommon();
		SeatAssignmentResponse expectedResponse = SeatAssignmentResponse.of(
			MATCH_ID, cpBlock, List.of(), Instant.parse("2026-04-15T10:05:00Z"), false);
		given(seatAssignmentTransactionalService.assignAndHold(eq(USER_ID), eq(MATCH_ID), eq(99L), any(Block.class), eq(3), eq(false)))
			.willReturn(expectedResponse);

		// when
		SeatAssignmentResponse response = seatAssignmentService.assignAndHoldSeats(USER_ID, MATCH_ID, BLOCK_NUM);

		// then
		assertThat(response).isNotNull();
		then(blockRepository).should().findByBlockNumWithSectionOrThrow(BLOCK_NUM);
		then(seatAssignmentTransactionalService).should().assignAndHold(eq(USER_ID), eq(MATCH_ID), eq(99L), any(Block.class), eq(3), eq(false));
		then(seatBlockLock).should().unlock(MATCH_ID, 99L);
	}

	@Test
	@DisplayName("락 획득 실패 시 예외가 발생한다")
	void 락_획득_실패_예외() {
		// given
		cpBlock = BlockFixture.cpBlock();
		ReflectionTestUtils.setField(cpBlock, "id", 99L);
		given(bookingOptionsRedisRepository.getByUserIdAndMatchIdOrThrow(USER_ID, MATCH_ID))
			.willReturn(new BookingOptions(USER_ID, MATCH_ID, true, 3, false, Instant.now()));
		given(blockRepository.findByBlockNumWithSectionOrThrow(BLOCK_NUM)).willReturn(cpBlock);
		given(seatBlockLock.tryLock(MATCH_ID, 99L)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> seatAssignmentService.assignAndHoldSeats(USER_ID, MATCH_ID, BLOCK_NUM))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SEAT_LOCK_ACQUISITION_FAILED);
	}

	@Test
	@DisplayName("트랜잭션 서비스에서 예외 발생 시에도 락이 해제된다")
	void 트랜잭션_예외시_락_해제() {
		// given
		setupCommon();
		given(seatAssignmentTransactionalService.assignAndHold(eq(USER_ID), eq(MATCH_ID), eq(99L), any(), eq(3), eq(false)))
			.willThrow(new CustomException(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE));

		// when & then
		assertThatThrownBy(() -> seatAssignmentService.assignAndHoldSeats(USER_ID, MATCH_ID, BLOCK_NUM))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.NO_CONSECUTIVE_SEAT_AVAILABLE);

		then(seatBlockLock).should().unlock(MATCH_ID, 99L);
	}
}
