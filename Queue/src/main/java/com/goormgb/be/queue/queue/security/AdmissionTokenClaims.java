package com.goormgb.be.queue.queue.security;

import java.time.Instant;

public record AdmissionTokenClaims(
	Long userId,
	Long matchId,
	String tokenId,
	Instant issuedAt,
	Instant expiresAt
) {
}
