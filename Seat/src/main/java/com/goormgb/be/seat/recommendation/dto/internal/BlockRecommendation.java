package com.goormgb.be.seat.recommendation.dto.internal;

import com.goormgb.be.seat.block.entity.Block;

public record BlockRecommendation(
	Block block,
	int realConsecutiveCount,
	int semiConsecutiveCount,
	long remainingSeatCount
) {
	public int combinedCount() {
		return realConsecutiveCount + semiConsecutiveCount;
	}
}
