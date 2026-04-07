package com.goormgb.be.kafka.event;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlockedEvent {

	private Long userId;
	private Instant occurredAt;
}
