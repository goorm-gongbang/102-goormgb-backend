package com.goormgb.be.seat.redis;

import java.io.Serializable;

import com.goormgb.be.seat.booking.model.BookingOptions;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SeatSession implements Serializable {

	private Long userId;
	private Long matchId;

	private boolean recommendationEnabled;

	private int ticketCount;

	private boolean nearAdjacentToggle;

	public SeatSession(
		Long userId,
		Long matchId,
		boolean recommendationEnabled,
		int ticketCount,
		boolean nearAdjacentToggle
	) {
		this.userId = userId;
		this.matchId = matchId;
		this.recommendationEnabled = recommendationEnabled;
		this.ticketCount = ticketCount;
		this.nearAdjacentToggle = nearAdjacentToggle;
	}

	public static SeatSession from(BookingOptions options) {
		return new SeatSession(
			options.userId(),
			options.matchId(),
			options.recommendationEnabled(),
			options.ticketCount(),
			options.nearAdjacentToggle()
		);
	}
}
