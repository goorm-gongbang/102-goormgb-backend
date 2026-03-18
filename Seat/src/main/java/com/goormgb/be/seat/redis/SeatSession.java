package com.goormgb.be.seat.redis;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SeatSession implements Serializable {

	private Long userId;
	private Long matchId;

	private boolean recommendationEnabled;

	private int ticketCount;

	public SeatSession(
		Long userId,
		Long matchId,
		boolean recommendationEnabled,
		int ticketCount
	) {
		this.userId = userId;
		this.matchId = matchId;
		this.recommendationEnabled = recommendationEnabled;
		this.ticketCount = ticketCount;
	}

}