package com.goormgb.be.queue.queue.dto.response;

import com.goormgb.be.queue.queue.enums.QueueStatus;

public record QueueEnterResponse(
	QueueStatus status,
	long rank,
	long totalWaitingCount
) {
	public static QueueEnterResponse waiting(long rank, long totalWaitingCount) {
		return new QueueEnterResponse(QueueStatus.WAITING, rank, totalWaitingCount);
	}
}
