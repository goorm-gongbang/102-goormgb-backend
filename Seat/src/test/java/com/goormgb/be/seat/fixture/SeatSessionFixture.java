package com.goormgb.be.seat.fixture;

import com.goormgb.be.seat.redis.SeatSession;

public final class SeatSessionFixture {

	private SeatSessionFixture() {
	}

	public static SeatSession of(Long userId, Long matchId, boolean recommendationEnabled, int ticketCount) {
		return new SeatSession(userId, matchId, recommendationEnabled, ticketCount);
	}

	public static SeatSession defaultSession(int ticketCount) {
		return new SeatSession(1L, 1L, true, ticketCount);
	}
}
