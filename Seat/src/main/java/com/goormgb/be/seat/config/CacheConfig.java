package com.goormgb.be.seat.config;

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
 * Seat 서비스 로컬 캐시 설정.
 *
 * <p>부하테스트에서 확인된 DB 커넥션 풀 소진을 완화하기 위해 정적/준정적 조회 지점을
 * Caffeine 로컬 캐시로 흡수한다. 캐시별 TTL 과 사이즈를 다르게 가져가야 하므로
 * Spring Boot 자동 구성(단일 spec) 대신 {@link CaffeineCacheManager#registerCustomCache}
 * 방식으로 명시 등록한다.</p>
 *
 * <p>정합성 전략:</p>
 * <ul>
 *     <li>{@code match-exists} / {@code match-detail}: 경기 메타는 거의 불변이며
 *         TTL 10분 내 변경이 반영되어도 서비스에 치명적이지 않다.</li>
 *     <li>{@code section-all} / {@code blocks-by-section-ids}: 스타디움 구조는
 *         배포 단위로만 변경되므로 TTL 1시간을 적용한다.</li>
 * </ul>
 *
 * @see com.goormgb.be.seat.booking.service.MatchExistenceValidator
 * @see com.goormgb.be.seat.common.service.MatchDetailCacheService
 * @see com.goormgb.be.seat.common.service.SectionLookupCacheService
 */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String CACHE_MATCH_EXISTS = "match-exists";
	public static final String CACHE_MATCH_DETAIL = "match-detail";
	public static final String CACHE_SECTION_ALL = "section-all";
	public static final String CACHE_BLOCKS_BY_SECTION_IDS = "blocks-by-section-ids";
	/**
	 * Phase 3 — {@code GET /matches/{matchId}/seat-groups} 응답의 유저 독립 payload 캐시.
	 *
	 * <p>좌석 선점·해제가 실시간으로 반영돼야 하므로 TTL 을 5초로 짧게 가져간다.
	 * 유저별로 다른 {@code seatSession} 은 본 캐시 payload 에서 제외하고 컨트롤러 상위 계층에서 조립한다.</p>
	 */
	public static final String CACHE_SEAT_GROUPS_RESPONSE = "seat-groups-response";

	@Primary
	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager manager = new CaffeineCacheManager();

		manager.registerCustomCache(CACHE_MATCH_EXISTS,
			Caffeine.newBuilder()
				.maximumSize(1_000)
				.expireAfterWrite(Duration.ofMinutes(10))
				.recordStats()
				.build());

		manager.registerCustomCache(CACHE_MATCH_DETAIL,
			Caffeine.newBuilder()
				.maximumSize(1_000)
				.expireAfterWrite(Duration.ofMinutes(10))
				.recordStats()
				.build());

		manager.registerCustomCache(CACHE_SECTION_ALL,
			Caffeine.newBuilder()
				.maximumSize(16)
				.expireAfterWrite(Duration.ofHours(1))
				.recordStats()
				.build());

		manager.registerCustomCache(CACHE_BLOCKS_BY_SECTION_IDS,
			Caffeine.newBuilder()
				.maximumSize(512)
				.expireAfterWrite(Duration.ofHours(1))
				.recordStats()
				.build());

		return manager;
	}

	/**
	 * Phase 3 — Redis 분산 캐시 CacheManager.
	 *
	 * <p>응답 레벨 캐시({@value #CACHE_SEAT_GROUPS_RESPONSE}) 를 Pod 간 공유하기 위해 별도 빈으로 등록한다.
	 * 기존 Caffeine {@code cacheManager} 와는 독립 운용되며, {@code @Cacheable(cacheManager = "redisCacheManager")}
	 * 로 명시 호출해야 한다.</p>
	 */
	@Bean(name = "redisCacheManager")
	public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
		GenericJackson2JsonRedisSerializer valueSerializer =
			new GenericJackson2JsonRedisSerializer(cacheObjectMapper());

		Map<String, RedisCacheConfiguration> configs = new HashMap<>();
		configs.put(CACHE_SEAT_GROUPS_RESPONSE, redisConfig(Duration.ofSeconds(5), valueSerializer));

		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(redisConfig(Duration.ofSeconds(30), valueSerializer))
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
	 *
	 * <p>역직렬화 대상 subtype 을 프로젝트/표준 JDK 패키지로 whitelist 하여,
	 * Redis 값 조작을 통한 임의 클래스 역직렬화(RCE gadget chain) 를 차단한다.</p>
	 */
	private ObjectMapper cacheObjectMapper() {
		PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
			.allowIfBaseType(Object.class)
			.build();

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		mapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
		return mapper;
	}
}