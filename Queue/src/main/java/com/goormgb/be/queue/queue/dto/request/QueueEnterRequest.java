package com.goormgb.be.queue.queue.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record QueueEnterRequest(
	boolean recommendationEnabled,

	@Min(1)
	@Max(10)
	int ticketCount
) {
}
