package com.goormgb.be.queue.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Queue 서비스 로컬 캐시 설정.
 *
 * <p>대기열 진입({@code QueueService#enter}) 시 매번 수행되던
 * {@code MatchRepository.findByIdOrThrow} 쿼리를 로컬 캐시로 흡수해 DB 커넥션 풀
 * 소진을 완화한다. 대기열 오픈 시각 전후의 {@code saleStatus} 전환이 반영되기까지
 * 최대 1분 지연이 허용되도록 TTL 을 짧게 가져간다.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String CACHE_MATCH_FOR_QUEUE = "match-for-queue";

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager manager = new CaffeineCacheManager();

		manager.registerCustomCache(CACHE_MATCH_FOR_QUEUE,
			Caffeine.newBuilder()
				.maximumSize(1_000)
				.expireAfterWrite(Duration.ofMinutes(1))
				.recordStats()
				.build());

		return manager;
	}
}
