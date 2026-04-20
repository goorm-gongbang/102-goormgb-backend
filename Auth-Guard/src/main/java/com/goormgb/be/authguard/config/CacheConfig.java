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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
		GenericJackson2JsonRedisSerializer valueSerializer =
			new GenericJackson2JsonRedisSerializer(cacheObjectMapper());

		Map<String, RedisCacheConfiguration> configs = new HashMap<>();
		configs.put(CACHE_USER_BY_ID, redisConfig(Duration.ofMinutes(10), valueSerializer));
		configs.put(CACHE_AUTH_ME, redisConfig(Duration.ofSeconds(30), valueSerializer));

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
	 * Redis 캐시 값 직렬화에 사용할 전용 {@link ObjectMapper} 를 생성한다.
	 *
	 * <p>기본 {@code GenericJackson2JsonRedisSerializer} 는 {@link java.time.Instant} 등
	 * JSR-310 타입을 직렬화하지 못해 런타임에 {@code Java 8 date/time type not supported}
	 * 예외가 발생한다. 이를 방지하기 위해 {@link JavaTimeModule} 을 등록한 전용 매퍼를 사용한다.</p>
	 *
	 * <p>Polymorphic typing 은 {@code GenericJackson2JsonRedisSerializer} 동작에 필요하므로
	 * {@link BasicPolymorphicTypeValidator} 로 도메인 패키지만 허용하도록 제한해 안전하게 활성화한다.</p>
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
