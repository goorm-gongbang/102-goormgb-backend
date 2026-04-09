package com.goormgb.be.seat.booking.repository;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.seat.booking.model.BookingOptions;

@Repository
public class BookingOptionsRedisRepository {

	private final String keyPrefix;
	private final long ttlSeconds;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper redisObjectMapper;

	public BookingOptionsRedisRepository(
		@Qualifier("stringRedisTemplate") StringRedisTemplate redisTemplate,
		@Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper,
		@Value("${booking.options-key-prefix}") String keyPrefix,
		@Value("${booking.options-ttl-seconds}") long ttlSeconds
	) {
		this.redisTemplate = redisTemplate;
		this.redisObjectMapper = redisObjectMapper;
		this.keyPrefix = keyPrefix;
		this.ttlSeconds = ttlSeconds;
	}

	public void save(BookingOptions options) {
		String key = generateKey(options.matchId(), options.userId());
		try {
			String json = redisObjectMapper.writeValueAsString(options);
			redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
		} catch (IOException e) {
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
				"예매 옵션 직렬화 실패: key=" + key, e);
		}
	}

	public BookingOptions getByUserIdAndMatchIdOrThrow(Long userId, Long matchId) {
		String key = generateKey(matchId, userId);
		String raw = redisTemplate.opsForValue().get(key);

		Preconditions.validate(raw != null, ErrorCode.SEAT_SESSION_NOT_FOUND);

		try {
			return redisObjectMapper.readValue(raw, BookingOptions.class);
		} catch (IOException e) {
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
				"예매 옵션 역직렬화 실패: key=" + key, e);
		}
	}

	public void delete(Long matchId, Long userId) {
		redisTemplate.delete(generateKey(matchId, userId));
	}

	private String generateKey(Long matchId, Long userId) {
		return keyPrefix + ":" + matchId + ":" + userId;
	}
}
