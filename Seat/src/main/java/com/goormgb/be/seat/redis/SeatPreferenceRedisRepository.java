package com.goormgb.be.seat.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SeatPreferenceRedisRepository {

	private final RedisTemplate<String, SeatSession> redisTemplate;

	private static final String KEY_PREFIX = "seat:preference:";

	public SeatSession getByUserIdAndMatchIdOrThrow(Long userId, Long matchId) {
		String key = generateKey(userId, matchId);

		SeatSession seatSession = redisTemplate.opsForValue().get(key);

		Preconditions.validate(seatSession != null, ErrorCode.SEAT_SESSION_NOT_FOUND);

		return seatSession;
	}

	private String generateKey(Long userId, Long matchId) {
		return KEY_PREFIX + userId + ":" + matchId;
	}

}
