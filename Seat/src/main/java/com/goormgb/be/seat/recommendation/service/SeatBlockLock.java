package com.goormgb.be.seat.recommendation.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Redis SETNX 기반 블럭 단위 분산 락.
 *
 * <p>좌석 배정 시 동일 블럭에 대한 동시 접근을 방지한다.
 * 락 획득에 실패하면 다른 사용자가 해당 블럭에서 좌석을 선택 중임을 의미한다.</p>
 */
@Component
@RequiredArgsConstructor
public class SeatBlockLock {

	private static final String LOCK_KEY_PREFIX = "block_lock:";
	private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(5);

	private final StringRedisTemplate redisTemplate;

	public boolean tryLock(Long blockId) {
		String key = LOCK_KEY_PREFIX + blockId;
		return Boolean.TRUE.equals(
			redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", LOCK_TIMEOUT)
		);
	}

	public void unlock(Long blockId) {
		String key = LOCK_KEY_PREFIX + blockId;
		redisTemplate.delete(key);
	}
}
