package com.goormgb.be.apigateway.jwt.repository;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Auth-Guard에서 등록한 블랙리스트 jti를 Redis에서 조회하는 Repository.
 * Key 구조: token_blacklist:{jti}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AccessTokenBlacklistRepository {

	private static final String KEY_PREFIX = "token_blacklist:";

	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	public Mono<Boolean> isBlacklisted(String jti) {
		return reactiveRedisTemplate.hasKey(KEY_PREFIX + jti)
				.doOnNext(result -> log.debug("Blacklist check - jti: {}, blacklisted: {}", jti, result));
	}
}
