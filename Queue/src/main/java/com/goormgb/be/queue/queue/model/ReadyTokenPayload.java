package com.goormgb.be.queue.queue.model;

import java.time.Instant;

public record ReadyTokenPayload(
	Long userId,
	Long matchId,
	String admissionToken,
	Instant issuedAt,
	Instant expiresAt
) {
}
