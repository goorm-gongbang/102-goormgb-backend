package com.goormgb.be.seat.common.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.common.dto.response.SeatHoldCreateResponse;
import com.goormgb.be.seat.common.service.lock.SeatHoldLockManager;
import com.goormgb.be.seat.fixture.SeatSessionFixture;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;

@ExtendWith(MockitoExtension.class)
class SeatHoldServiceTest {

	private static final Long USER_ID = 7L;
	private static final Long MATCH_ID = 10L;

	@Mock
	private SeatPreferenceRedisRepository seatPreferenceRedisRepository;
	@Mock
	private SeatHoldLockManager seatHoldLockManager;
	@Mock
	private SeatHoldTransactionalService seatHoldTransactionalService;

	@InjectMocks
	private SeatHoldService seatHoldService;

	private void setupSession(int ticketCount) {
		given(seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(USER_ID, MATCH_ID))
			.willReturn(SeatSessionFixture.of(USER_ID, MATCH_ID, true, ticketCount));
	}

	@Test
	@DisplayName("seatIds가 중복이면 INVALID_SEAT_HOLD_REQUEST 예외가 발생한다")
	void 중복_좌석_요청_예외() {
		// when & then
		assertThatThrownBy(() -> seatHoldService.createOrRefreshHold(USER_ID, MATCH_ID, List.of(1L, 1L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_SEAT_HOLD_REQUEST);

		verifyNoInteractions(seatHoldLockManager);
	}

	@Test
	@DisplayName("seatIds가 null이면 INVALID_SEAT_HOLD_REQUEST 예외가 발생한다")
	void null_좌석_요청_예외() {
		// when & then
		assertThatThrownBy(() -> seatHoldService.createOrRefreshHold(USER_ID, MATCH_ID, null))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_SEAT_HOLD_REQUEST);

		verifyNoInteractions(seatHoldLockManager);
	}

	@Test
	@DisplayName("티켓 수와 좌석 수가 다르면 INVALID_SEAT_HOLD_REQUEST 예외가 발생한다")
	void 티켓수_불일치_예외() {
		// given
		setupSession(3);

		// when & then
		assertThatThrownBy(() -> seatHoldService.createOrRefreshHold(USER_ID, MATCH_ID, List.of(1L, 2L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_SEAT_HOLD_REQUEST);

		verifyNoInteractions(seatHoldLockManager);
	}

	@Test
	@DisplayName("정상 요청 시 락 획득 후 트랜잭션 서비스를 호출하고 락을 해제한다")
	void 정상_요청_락_트랜잭션_순서() {
		// given
		setupSession(2);
		RLock lock1 = mock(RLock.class);
		RLock lock2 = mock(RLock.class);
		given(seatHoldLockManager.lockAll(MATCH_ID, List.of(206313L, 206314L))).willReturn(List.of(lock1, lock2));

		SeatHoldCreateResponse expectedResponse = SeatHoldCreateResponse.of(
			MATCH_ID, List.of(206313L, 206314L), Instant.parse("2026-04-15T10:05:00Z"));
		given(seatHoldTransactionalService.createOrRefreshHold(USER_ID, MATCH_ID, List.of(206313L, 206314L)))
			.willReturn(expectedResponse);

		// when
		SeatHoldCreateResponse response = seatHoldService.createOrRefreshHold(USER_ID, MATCH_ID,
			List.of(206313L, 206314L));

		// then
		assertThat(response.matchId()).isEqualTo(MATCH_ID);
		assertThat(response.seatCount()).isEqualTo(2);

		then(seatHoldTransactionalService).should().createOrRefreshHold(USER_ID, MATCH_ID, List.of(206313L, 206314L));
		then(seatHoldLockManager).should().unlockAll(List.of(lock1, lock2));
	}

	@Test
	@DisplayName("트랜잭션 서비스에서 예외 발생 시에도 락이 해제된다")
	void 트랜잭션_예외시_락_해제() {
		// given
		setupSession(2);
		RLock lock1 = mock(RLock.class);
		RLock lock2 = mock(RLock.class);
		given(seatHoldLockManager.lockAll(MATCH_ID, List.of(206313L, 206314L))).willReturn(List.of(lock1, lock2));

		given(seatHoldTransactionalService.createOrRefreshHold(USER_ID, MATCH_ID, List.of(206313L, 206314L)))
			.willThrow(new CustomException(ErrorCode.SEAT_ALREADY_HELD_BY_OTHER));

		// when & then
		assertThatThrownBy(() -> seatHoldService.createOrRefreshHold(USER_ID, MATCH_ID, List.of(206313L, 206314L)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SEAT_ALREADY_HELD_BY_OTHER);

		then(seatHoldLockManager).should().unlockAll(List.of(lock1, lock2));
	}
}
