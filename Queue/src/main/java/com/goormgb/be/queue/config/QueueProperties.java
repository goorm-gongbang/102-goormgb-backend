package com.goormgb.be.queue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue")
public record QueueProperties(
	Long readyTtlSeconds,
	Integer promotionBatchSize,
	Long promotionIntervalMs,
	String activeMatchKey,
	String waitKeyPrefix,
	String readyKeyPrefix,
	String preferenceKeyPrefix
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
}
