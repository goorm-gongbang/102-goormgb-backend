package com.goormgb.be.seat.fixture;

import java.time.Instant;

import com.goormgb.be.seat.booking.model.BookingOptions;

public final class BookingOptionsFixture {

	private BookingOptionsFixture() {
	}

	public static BookingOptions of(Long userId, Long matchId, boolean recommendationEnabled, int ticketCount,
		boolean nearAdjacentToggle) {
		return new BookingOptions(userId, matchId, recommendationEnabled, ticketCount, nearAdjacentToggle, Instant.now());
	}

	public static BookingOptions defaultOptions(int ticketCount) {
		return new BookingOptions(1L, 1L, true, ticketCount, false, Instant.now());
	}
}
