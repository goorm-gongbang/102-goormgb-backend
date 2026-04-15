package com.goormgb.be.authguard.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Auth-Guard 서비스 캐시 설정 (Phase 2: Redis 분산 캐시).
 *
 * <p>Auth-Guard 는 Phase 1 의 Caffeine 로컬 캐시 대상이 없었기 때문에 본 클래스가 최초
 * {@code CacheManager} 빈 구성이다. 사용자 정보는 Pod 간 즉시 전파(닉네임/차단 상태 변경)가
 * 필요하므로 로컬 캐시가 아닌 Redis 분산 캐시를 채택한다.</p>
 *
 * <p>등록된 캐시:</p>
 * <ul>
 *   <li>{@value #CACHE_USER_BY_ID} — {@code UserRepository.findByIdOrThrow} 결과의 DTO 스냅샷. TTL 10분.</li>
 *   <li>{@value #CACHE_AUTH_ME} — {@code /me} 응답 DTO. TTL 30초 (프론트 폴링 허용 범위).</li>
 * </ul>
 *
 * <p>정합성 전략: 프로필/상태 변경 서비스 메서드에 {@code @CacheEvict} 를 걸어 즉시 무효화한다.
 * Redis 장애 시 Spring Cache 기본 동작에 따라 캐시 미스로 fallback 되어 DB 직접 조회가 수행된다.</p>
 *
 * @see com.goormgb.be.user.dto.cache.UserCacheDto
 */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String CACHE_USER_BY_ID = "user-by-id";
	public static final String CACHE_AUTH_ME = "auth-me";

	@Primary
	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		Map<String, RedisCacheConfiguration> configs = new HashMap<>();
		configs.put(CACHE_USER_BY_ID, redisConfig(Duration.ofMinutes(10)));
		configs.put(CACHE_AUTH_ME, redisConfig(Duration.ofSeconds(30)));

		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(redisConfig(Duration.ofMinutes(10)))
			.withInitialCacheConfigurations(configs)
			.build();
	}

	private RedisCacheConfiguration redisConfig(Duration ttl) {
		return RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(ttl)
			.serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
			.disableCachingNullValues();
	}
}
