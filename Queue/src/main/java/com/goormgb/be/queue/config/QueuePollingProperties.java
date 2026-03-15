package com.goormgb.be.queue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue.polling")
public record QueuePollingProperties(
	long readyMs,
	long fastMs,
	long mediumMs,
	long slowMs,
	long fastRankThreshold,
	long mediumRankThreshold
) {
}
