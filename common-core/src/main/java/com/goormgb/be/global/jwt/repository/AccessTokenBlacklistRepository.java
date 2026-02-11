package com.goormgb.be.global.jwt.repository;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그아웃된 Access Token의 jti를 Redis에 블랙리스트로 저장하는 Repository.
 * <p>
 * Key 구조: {@code token_blacklist:{jti}}
 * <p>
 * TTL: Access Token의 남은 만료 시간만큼만 저장하여 불필요한 메모리 사용을 방지한다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AccessTokenBlacklistRepository {

	private static final String KEY_PREFIX = "token_blacklist:";
	private static final String BLACKLISTED_VALUE = "blacklisted";

	private final StringRedisTemplate redisTemplate;

	/**
	 * Access Token의 jti를 블랙리스트에 등록한다.
	 *
	 * @param jti Access Token의 고유 식별자
	 * @param ttl 남은 만료 시간
	 */
	public void save(String jti, Duration ttl) {
		String key = generateKey(jti);
		redisTemplate.opsForValue().set(key, BLACKLISTED_VALUE, ttl);
		log.debug("Access token blacklisted - jti: {}", jti);
	}

	/**
	 * jti가 블랙리스트에 존재하는지 확인한다.
	 *
	 * @param jti Access Token의 고유 식별자
	 * @return 블랙리스트 존재 여부
	 */
	public boolean existsByJti(String jti) {
		String key = generateKey(jti);
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	private String generateKey(String jti) {
		return KEY_PREFIX + jti;
	}
}