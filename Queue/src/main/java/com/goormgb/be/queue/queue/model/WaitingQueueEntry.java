package com.goormgb.be.queue.queue.model;

public record WaitingQueueEntry(
	Long userId,
	long enteredAtMillis
) {
	public static WaitingQueueEntry of(Long userId, long enteredAtMillis) {
		return new WaitingQueueEntry(userId, enteredAtMillis);
	}
}
