package com.goormgb.be.seat.booking.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.goormgb.be.domain.match.repository.MatchRepository;
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

	private final MatchRepository matchRepository;
	private final BookingOptionsRedisRepository bookingOptionsRedisRepository;

	public BookingOptionsResponse saveBookingOptions(Long matchId, Long userId, BookingOptionsRequest request) {
		matchRepository.findByIdOrThrow(matchId, ErrorCode.MATCH_NOT_FOUND);

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

		return new BookingOptionsResponse(
			matchId,
			options.recommendationEnabled(),
			options.ticketCount(),
			options.nearAdjacentToggle()
		);
	}
}
