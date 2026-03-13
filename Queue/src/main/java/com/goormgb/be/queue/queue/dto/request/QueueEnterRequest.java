package com.goormgb.be.queue.queue.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record QueueEnterRequest(
	boolean recommendationEnabled,

	@Min(1)
	@Max(10)
	int ticketCount,

	@Size(max = 10)
	List<Long> preferredBlockIds
) {
}
