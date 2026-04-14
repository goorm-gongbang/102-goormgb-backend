package com.goormgb.be.seat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Spring Cache 추상화를 활성화하기 위한 설정.
 *
 * <p>캐시 인스턴스 자체는 {@code spring.cache.*} YAML 설정을 통해
 * Spring Boot의 {@code CaffeineCacheManager} 자동 구성으로 생성된다.
 * 본 클래스는 {@link EnableCaching}만 선언하여 애플리케이션 전역에서
 * {@code @Cacheable} 등 캐시 어노테이션이 동작하도록 한다.</p>
 *
 * @see com.goormgb.be.seat.booking.service.MatchExistenceValidator
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
