package com.goormgb.be.global.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeatPreferenceCache(
	Long userId,
	Long matchId,
	boolean recommendationEnabled,
	int ticketCount,
	Instant enteredAt
) {
}
