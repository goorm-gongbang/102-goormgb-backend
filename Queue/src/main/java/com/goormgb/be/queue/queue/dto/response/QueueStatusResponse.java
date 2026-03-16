package com.goormgb.be.queue.queue.dto.response;

import com.goormgb.be.queue.queue.enums.QueueStatus;

public record QueueStatusResponse(
	QueueStatus status,
	Long rank,
	Long totalWaitingCount,
	String admissionToken,
	Long expiresIn,
	Long pollingMs
) {
	public static QueueStatusResponse waiting(long rank, long totalWaitingCount, long pollingMs) {
		return new QueueStatusResponse(QueueStatus.WAITING, rank, totalWaitingCount, null, null, pollingMs);
	}

	public static QueueStatusResponse ready(String admissionToken, long expiresIn) {
		return new QueueStatusResponse(QueueStatus.READY, null, null, admissionToken, expiresIn, null);
	}
}
