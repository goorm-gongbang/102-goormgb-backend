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
import com.goormgb.be.seat.booking.repository.PreQueueBookingOptionMarkerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingOptionsService {

	private static final int MARK_RETRY_MAX_ATTEMPTS = 3;
	private static final long MARK_RETRY_SLEEP_MILLIS = 50L;

	private final MatchExistenceValidator matchExistenceValidator;
	private final BookingOptionsRedisRepository bookingOptionsRedisRepository;
	private final PreQueueBookingOptionMarkerRepository preQueueBookingOptionMarkerRepository;

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

	private void syncPreQueueMarkerOrRollback(Long matchId, Long userId) {
		RuntimeException lastException = null;

		for (int attempt = 1; attempt <= MARK_RETRY_MAX_ATTEMPTS; attempt++) {
			try {
				preQueueBookingOptionMarkerRepository.mark(matchId, userId);
				return;
			} catch (RuntimeException e) {
				lastException = e;
				if (attempt < MARK_RETRY_MAX_ATTEMPTS) {
					sleepBeforeRetry(attempt);
				}
			}
		}

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
			lastException
		);
	}

	private void sleepBeforeRetry(int attempt) {
		try {
			Thread.sleep(MARK_RETRY_SLEEP_MILLIS * attempt);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "prequeue 마커 재시도 중 인터럽트가 발생했습니다.", e);
		}
	}
}
