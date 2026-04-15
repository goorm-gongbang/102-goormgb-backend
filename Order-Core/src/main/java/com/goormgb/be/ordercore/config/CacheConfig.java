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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
	/**
	 * Auth-Guard 가 소유한 {@code /auth/me} 응답 캐시의 키 스페이스.
	 *
	 * <p>Order-Core 는 해당 캐시에 값을 <b>저장하지 않고</b>, 사용자 프로필·온보딩 변경 시 동일
	 * Redis 인스턴스에서 키를 삭제하기 위한 용도로만 등록한다. 같은 사용자에 대한 쓰기가 Order-Core
	 * 에서 발생해도 Auth-Guard 의 {@code /me} 응답이 즉시 갱신되도록 보장한다.</p>
	 */
	public static final String CACHE_AUTH_ME = "auth-me";
	/**
	 * Phase 3 — {@code GET /matches?date=...} 응답의 Redis 분산 캐시.
	 *
	 * <p>경기 목록은 자주 폴링되는 read-only 화면이며 동일 날짜 요청이 집중된다. TTL 30초로 캐싱한다.</p>
	 */
	public static final String CACHE_MATCHES_LIST_RESPONSE = "matches-list-response";

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
		GenericJackson2JsonRedisSerializer valueSerializer =
			new GenericJackson2JsonRedisSerializer(cacheObjectMapper());

		Map<String, RedisCacheConfiguration> configs = new HashMap<>();
		configs.put(CACHE_USER_BY_ID, redisConfig(Duration.ofMinutes(10), valueSerializer));
		configs.put(CACHE_AUTH_ME, redisConfig(Duration.ofSeconds(30), valueSerializer));
		configs.put(CACHE_MATCHES_LIST_RESPONSE, redisConfig(Duration.ofSeconds(30), valueSerializer));

		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(redisConfig(Duration.ofMinutes(10), valueSerializer))
			.withInitialCacheConfigurations(configs)
			.build();
	}

	private RedisCacheConfiguration redisConfig(Duration ttl, GenericJackson2JsonRedisSerializer serializer) {
		return RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(ttl)
			.serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(SerializationPair.fromSerializer(serializer))
			.disableCachingNullValues();
	}

	/**
	 * Redis 캐시 값 직렬화 전용 {@link ObjectMapper}.
	 *
	 * <p>{@link java.time.Instant} 등 JSR-310 타입 직렬화 지원을 위해 {@link JavaTimeModule} 을 등록하고,
	 * {@code GenericJackson2JsonRedisSerializer} 가 요구하는 polymorphic typing 을
	 * {@link BasicPolymorphicTypeValidator} 로 제한 활성화한다.</p>
	 */
	private ObjectMapper cacheObjectMapper() {
		PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
			.allowIfSubType("com.goormgb.be")
			.allowIfSubType("java.util")
			.allowIfSubType("java.time")
			.allowIfSubType("java.lang")
			.build();

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		mapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
		return mapper;
	}
}
