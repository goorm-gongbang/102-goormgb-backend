package com.goormgb.be.seat.redis;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SeatSession implements Serializable {

	private Long userId;
	private Long matchId;

	private boolean recommendationEnabled;

	private int ticketCount;

	private List<Long> preferredBlockIds;

}