package com.goormgb.be.seat.redis;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.model.SeatPreferenceCache;
import com.goormgb.be.global.support.Preconditions;

@Repository
public class SeatPreferenceRedisRepository {

	private final String preferenceKeyPrefix;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper redisObjectMapper;

	public SeatPreferenceRedisRepository(
		StringRedisTemplate redisTemplate,
		@Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper,
		@Value("${queue.preference-key-prefix}") String preferenceKeyPrefix
	) {
		this.redisTemplate = redisTemplate;
		this.redisObjectMapper = redisObjectMapper;
		this.preferenceKeyPrefix = preferenceKeyPrefix;
	}

	public SeatSession getByUserIdAndMatchIdOrThrow(Long userId, Long matchId) {
		String key = generateQueueKey(userId, matchId);

		SeatSession seatSession = getByKey(key);

		Preconditions.validate(seatSession != null, ErrorCode.SEAT_SESSION_NOT_FOUND);

		return seatSession;
	}

	private SeatSession getByKey(String key) {
		String raw = redisTemplate.opsForValue().get(key);
		if (raw == null) {
			return null;
		}

		try {
			SeatPreferenceCache cache = redisObjectMapper.readValue(raw, SeatPreferenceCache.class);
			return new SeatSession(
				cache.userId(),
				cache.matchId(),
				cache.recommendationEnabled(),
				cache.ticketCount(),
				cache.preferredBlockIds()
			);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to deserialize redis seat preference for key: " + key, e);
		}
	}

	private String generateQueueKey(Long userId, Long matchId) {
		return preferenceKeyPrefix + ":" + matchId + ":" + userId;
	}
}