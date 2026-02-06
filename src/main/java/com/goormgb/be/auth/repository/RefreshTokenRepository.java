package com.goormgb.be.auth.repository;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormgb.be.auth.config.JwtProperties;
import com.goormgb.be.auth.dto.RefreshTokenInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * Refresh Token을 Redis에 저장/조회/삭제하는 Repository.
 * <p>
 * Key 구조: {@code refresh_token:{jti}}
 * <p>
 * 중복 로그인 허용: jti(토큰 고유 ID)를 키로 사용하여 유저당 여러 토큰 저장 가능
 */
@Slf4j
@Repository
public class RefreshTokenRepository {

	private static final String KEY_PREFIX = "refresh_token:";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final long ttlDays;

	public RefreshTokenRepository(StringRedisTemplate redisTemplate,
			ObjectMapper redisObjectMapper,
			JwtProperties jwtProperties) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = redisObjectMapper;
		this.ttlDays = jwtProperties.getRefreshToken().getExpirationDays();
	}

	/**
	 * Refresh Token 정보를 Redis에 저장한다.
	 *
	 * @param tokenInfo 저장할 토큰 정보 (jti 필수)
	 */
	public void save(RefreshTokenInfo tokenInfo) {
		String key = generateKey(tokenInfo.getJti());
		try {
			String json = objectMapper.writeValueAsString(tokenInfo);
			redisTemplate.opsForValue().set(key, json, Duration.ofDays(ttlDays));
			log.debug("Refresh token saved - jti: {}, userId: {}", tokenInfo.getJti(), tokenInfo.getUserId());
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize RefreshTokenInfo - jti: {}", tokenInfo.getJti(), e);
			throw new RuntimeException("Failed to save refresh token", e);
		}
	}

	/**
	 * jti로 Refresh Token 정보를 조회한다.
	 *
	 * @param jti 토큰 고유 식별자
	 * @return 토큰 정보 (없으면 empty)
	 */
	public Optional<RefreshTokenInfo> findByJti(String jti) {
		String key = generateKey(jti);
		String json = redisTemplate.opsForValue().get(key);

		if (json == null) {
			return Optional.empty();
		}

		try {
			RefreshTokenInfo tokenInfo = objectMapper.readValue(json, RefreshTokenInfo.class);
			return Optional.of(tokenInfo);
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize RefreshTokenInfo - jti: {}", jti, e);
			return Optional.empty();
		}
	}

	/**
	 * jti로 Refresh Token을 삭제한다. (로그아웃 시 사용)
	 *
	 * @param jti 토큰 고유 식별자
	 */
	public void deleteByJti(String jti) {
		String key = generateKey(jti);
		Boolean deleted = redisTemplate.delete(key);
		if (Boolean.TRUE.equals(deleted)) {
			log.debug("Refresh token deleted - jti: {}", jti);
		} else {
			log.debug("No refresh token found to delete - jti: {}", jti);
		}
	}

	/**
	 * jti로 Refresh Token 존재 여부를 확인한다.
	 *
	 * @param jti 토큰 고유 식별자
	 * @return 존재 여부
	 */
	public boolean existsByJti(String jti) {
		String key = generateKey(jti);
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	private String generateKey(String jti) {
		return KEY_PREFIX + jti;
	}
}