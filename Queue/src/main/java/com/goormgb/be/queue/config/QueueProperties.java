package com.goormgb.be.queue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue")
public record QueueProperties(
	long readyTtlSeconds,
	int promotionBatchSize,
	long promotionIntervalMs,
	String activeMatchKey,
	String waitKeyPrefix,
	String readyKeyPrefix,
	String preferenceKeyPrefix,
	String expiredKeyPrefix
) {
	public String waitKey(Long matchId) {
		return waitKeyPrefix + ":" + matchId;
	}

	public String readyKey(Long matchId, Long userId) {
		return readyKeyPrefix + ":" + matchId + ":" + userId;
	}

	public String preferenceKey(Long matchId, Long userId) {
		return preferenceKeyPrefix + ":" + matchId + ":" + userId;
	}

	public String expiredKey(Long matchId, Long userId) {
		return expiredKeyPrefix + ":" + matchId + ":" + userId;
	}
}
