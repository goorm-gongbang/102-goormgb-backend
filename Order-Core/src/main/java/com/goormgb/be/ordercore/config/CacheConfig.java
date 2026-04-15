package com.goormgb.be.ordercore.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Order-Core 서비스 로컬 캐시 설정.
 *
 * <p>주문서 조회({@code OrderService#getOrderSheet}) 시 반복 수행되던 경기 메타 조회를
 * 흡수하기 위한 {@code match-detail} 캐시를 등록한다. 주문 생성({@code createOrder}) 의
 * {@code Match} 조회는 영속성 컨텍스트에 연결된 관리 엔티티가 필요하므로 캐시를 사용하지
 * 않는다.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String CACHE_MATCH_DETAIL = "match-detail";

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
}
