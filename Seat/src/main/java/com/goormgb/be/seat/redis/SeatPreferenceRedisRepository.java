package com.goormgb.be.seat.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.model.SeatPreferenceCache;
import com.goormgb.be.global.support.Preconditions;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SeatPreferenceRedisRepository {

	private final StringRedisTemplate redisTemplate;

	@Qualifier("redisObjectMapper")
	private final ObjectMapper redisObjectMapper;

	@Value("${queue.preference-key-prefix}")
	private String preferenceKeyPrefix;

	public SeatPreferenceCache getByUserIdAndMatchIdOrThrow(Long userId, Long matchId) {
		String key = generateKey(matchId, userId);
		String raw = redisTemplate.opsForValue().get(key);

		Preconditions.validate(raw != null, ErrorCode.SEAT_SESSION_NOT_FOUND);

		try {
			return redisObjectMapper.readValue(raw, SeatPreferenceCache.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to deserialize Redis value for key: " + key, e);
		}
	}

	private String generateKey(Long matchId, Long userId) {
		return preferenceKeyPrefix + ":" + matchId + ":" + userId;
	}
}