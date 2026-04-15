package com.goormgb.be.seat.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

	// recommendation/blocks 엔드포인트 튜닝용 — 유저 온보딩 설정은 거의 바뀌지 않으므로
	// 요청마다 DB 조회되던 것을 사용자 단위 local cache 로 흡수한다.
	public static final String CACHE_USER_PREFERENCE = "user-preference";
	public static final String CACHE_USER_PREFERRED_BLOCKS = "user-preferred-blocks";
	public static final String CACHE_USER_VIEWPOINT_PRIORITY = "user-viewpoint-priority";
	public static final String CACHE_BLOCKS_BY_BLOCK_NUMS = "blocks-by-block-nums";

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

		// 온보딩 유저 정보는 거의 불변 — TTL 10분, size 10k (부하테스트 토큰 rotation 고려)
		manager.registerCustomCache(CACHE_USER_PREFERENCE,
			Caffeine.newBuilder()
				.maximumSize(10_000)
				.expireAfterWrite(Duration.ofMinutes(10))
				.recordStats()
				.build());

		manager.registerCustomCache(CACHE_USER_PREFERRED_BLOCKS,
			Caffeine.newBuilder()
				.maximumSize(10_000)
				.expireAfterWrite(Duration.ofMinutes(10))
				.recordStats()
				.build());

		manager.registerCustomCache(CACHE_USER_VIEWPOINT_PRIORITY,
			Caffeine.newBuilder()
				.maximumSize(10_000)
				.expireAfterWrite(Duration.ofMinutes(10))
				.recordStats()
				.build());

		// 블록 정보는 스타디움 구조라 거의 불변 — TTL 1시간, 블록번호 조합별 key
		manager.registerCustomCache(CACHE_BLOCKS_BY_BLOCK_NUMS,
			Caffeine.newBuilder()
				.maximumSize(2_048)
				.expireAfterWrite(Duration.ofHours(1))
				.recordStats()
				.build());

		return manager;
	}
}