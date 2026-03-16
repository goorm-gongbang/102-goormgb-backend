package com.goormgb.be.queue.queue.policy;

import org.springframework.stereotype.Component;

import com.goormgb.be.queue.config.QueuePollingProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QueuePollingPolicy {
	private final QueuePollingProperties properties;

	public long forReady() {
		return properties.readyMs();
	}

	public long forWaiting(long rank) {
        if (rank <= properties.fastRankThreshold()) {
			return properties.fastMs();
		}
		if (rank <= properties.mediumRankThreshold()) {
			return properties.mediumMs();
		}
		return properties.slowMs();
	}
}
