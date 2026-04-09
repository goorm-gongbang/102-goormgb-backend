package com.goormgb.be.queue.queue.service;

import org.springframework.stereotype.Service;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.queue.queue.repository.PreQueueRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreQueueValidationService {

	private final PreQueueRedisRepository preQueueRedisRepository;

	public void validateBeforeEnter(Long matchId, Long userId) {
		boolean hasBookingOptions = preQueueRedisRepository.hasBookingOptions(matchId, userId);
		if (!hasBookingOptions) {
			log.warn(
				"Blocked queue enter request due to missing booking options. matchId={}, userId={}, reasonCode=MISSING_BOOKING_OPTIONS",
				matchId,
				userId
			);
		}
		Preconditions.validate(hasBookingOptions, ErrorCode.PREQUEUE_OPTIONS_REQUIRED);
	}
}
