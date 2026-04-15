package com.goormgb.be.seat.booking.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.seat.booking.dto.request.BookingOptionsRequest;
import com.goormgb.be.seat.booking.dto.response.BookingOptionsResponse;
import com.goormgb.be.seat.booking.model.BookingOptions;
import com.goormgb.be.seat.booking.repository.BookingOptionsRedisRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingOptionsService {

	private final MatchExistenceValidator matchExistenceValidator;
	private final BookingOptionsRedisRepository bookingOptionsRedisRepository;
	private final PreQueueMarkerRetryService preQueueMarkerRetryService;

	public BookingOptionsResponse saveBookingOptions(Long matchId, Long userId, BookingOptionsRequest request) {
		matchExistenceValidator.validateExists(matchId);

		if (request.recommendationEnabled()) {
			Preconditions.validate(request.ticketCount() != null, ErrorCode.INVALID_TICKET_COUNT);
		} else {
			Preconditions.validate(request.ticketCount() == null, ErrorCode.INVALID_BOOKING_OPTIONS);
			Preconditions.validate(!request.nearAdjacentToggle(), ErrorCode.INVALID_BOOKING_OPTIONS);
		}

		BookingOptions options = new BookingOptions(
			userId,
			matchId,
			request.recommendationEnabled(),
			request.ticketCount(),
			request.nearAdjacentToggle(),
			Instant.now()
		);

		bookingOptionsRedisRepository.save(options);
		syncPreQueueMarkerOrRollback(matchId, userId);

		return new BookingOptionsResponse(
			matchId,
			options.recommendationEnabled(),
			options.ticketCount(),
			options.nearAdjacentToggle()
		);
	}

	/**
	 * prequeue 마커 동기화. 실패 시 이미 저장된 bookingOptions 를 롤백한다.
	 *
	 * <p>재시도 자체는 {@link PreQueueMarkerRetryService} 가 Resilience4j 로 수행한다 (Phase 4).
	 * 기존의 {@code Thread.sleep} 기반 동기 블로킹 재시도는 Tomcat worker 스레드를
	 * 최대 300ms 점유하여 고부하 시 스레드 풀 고갈의 원인이 되었다.</p>
	 */
	private void syncPreQueueMarkerOrRollback(Long matchId, Long userId) {
		try {
			preQueueMarkerRetryService.mark(matchId, userId);
		} catch (RuntimeException markException) {
			try {
				bookingOptionsRedisRepository.delete(matchId, userId);
			} catch (RuntimeException rollbackException) {
				throw new CustomException(
					ErrorCode.PREQUEUE_MARKER_SYNC_FAILED,
					"예매 옵션 저장 후 prequeue 마커 동기화 및 롤백에 실패했습니다.",
					rollbackException
				);
			}
			throw new CustomException(
				ErrorCode.PREQUEUE_MARKER_SYNC_FAILED,
				"예매 옵션 저장 후 prequeue 마커 동기화에 실패하여 저장 내용을 롤백했습니다.",
				markException
			);
		}
	}
}
