package com.goormgb.be.seat.booking.repository;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PreQueueBookingOptionMarkerRepository {

	private final StringRedisTemplate preQueueStringRedisTemplate;
	private final String bookingOptionKeyPrefix;
	private final long ttlSeconds;

	public PreQueueBookingOptionMarkerRepository(
		@Qualifier("preQueueStringRedisTemplate") StringRedisTemplate preQueueStringRedisTemplate,
		@Value("${prequeue.booking-option-key-prefix:queue:precheck:booking-option}") String bookingOptionKeyPrefix,
		@Value("${prequeue.booking-option-ttl-seconds:${booking.options-ttl-seconds}}") long ttlSeconds
	) {
		this.preQueueStringRedisTemplate = preQueueStringRedisTemplate;
		this.bookingOptionKeyPrefix = bookingOptionKeyPrefix;
		this.ttlSeconds = ttlSeconds;
	}

	public void mark(Long matchId, Long userId) {
		preQueueStringRedisTemplate.opsForValue()
			.set(bookingOptionKey(matchId, userId), "1", Duration.ofSeconds(ttlSeconds));
	}

	private String bookingOptionKey(Long matchId, Long userId) {
		return bookingOptionKeyPrefix + ":" + matchId + ":" + userId;
	}
}
