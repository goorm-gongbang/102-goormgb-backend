package com.goormgb.be.queue.queue.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PreQueueRedisRepository {

	private final StringRedisTemplate redisTemplate;
	private final String bookingOptionKeyPrefix;

	public PreQueueRedisRepository(
		StringRedisTemplate redisTemplate,
		@Value("${prequeue.booking-option-key-prefix:queue:precheck:booking-option}") String bookingOptionKeyPrefix
	) {
		this.redisTemplate = redisTemplate;
		this.bookingOptionKeyPrefix = bookingOptionKeyPrefix;
	}

	public boolean hasBookingOptions(Long matchId, Long userId) {
		Boolean exists = redisTemplate.hasKey(bookingOptionKey(matchId, userId));
		return Boolean.TRUE.equals(exists);
	}

	private String bookingOptionKey(Long matchId, Long userId) {
		return bookingOptionKeyPrefix + ":" + matchId + ":" + userId;
	}
}
