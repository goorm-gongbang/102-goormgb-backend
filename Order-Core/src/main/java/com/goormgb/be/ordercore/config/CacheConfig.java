package com.goormgb.be.ordercore.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Order-Core 서비스 캐시 설정.
 *
 * <p>Phase 1 — Caffeine 로컬 캐시: 주문서 조회({@code OrderService#getOrderSheet}) 시 반복
 * 수행되던 경기 메타 조회를 흡수하기 위한 {@code match-detail} 캐시를 등록한다. 주문 생성
 * ({@code createOrder}) 의 {@code Match} 조회는 영속성 컨텍스트에 연결된 관리 엔티티가
 * 필요하므로 Caffeine 캐시를 사용하지 않는다.</p>
 *
 * <p>Phase 2 — Redis 분산 캐시: 사용자 정보({@code user-by-id}) 를 Pod 간 공유 목적으로
 * Redis 에 캐시한다. {@code @Cacheable} 어노테이션에서 {@code cacheManager = "redisCacheManager"}
 * 로 명시해 Caffeine(Primary) 와 격리 운용한다.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String CACHE_MATCH_DETAIL = "match-detail";
	public static final String CACHE_USER_BY_ID = "user-by-id";

	@Primary
	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager manager = new CaffeineCacheManager();

		manager.registerCustomCache(CACHE_MATCH_DETAIL,
			Caffeine.newBuilder()
				.maximumSize(1_000)
				.expireAfterWrite(Duration.ofMinutes(10))
				.recordStats()
				.build());

		return manager;
	}

	@Bean(name = "redisCacheManager")
	public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
		Map<String, RedisCacheConfiguration> configs = new HashMap<>();
		configs.put(CACHE_USER_BY_ID, redisConfig(Duration.ofMinutes(10)));

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
