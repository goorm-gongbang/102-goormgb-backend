package com.goormgb.be.ordercore.user.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.config.CacheConfig;
import com.goormgb.be.user.dto.cache.UserCacheDto;
import com.goormgb.be.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Order-Core 에서 사용자 정보를 Redis 분산 캐시를 경유해 조회하는 서비스.
 *
 * <p>주문 생성·마이페이지 프로필 등 요청당 발생하던 {@code UserRepository.findByIdOrThrow}
 * 호출을 흡수해 DB 부하를 줄인다. Auth-Guard 와 동일한 {@code user-by-id} 키 스페이스를
 * 공유하므로 Pod 간·서비스 간에도 일관된 사용자 스냅샷을 사용한다.</p>
 *
 * <p>캐시 기본 TTL 은 10분이며, 쓰기 트랜잭션(프로필 변경 등) 직후에는 {@link #evict(Long)} 으로
 * 즉시 무효화해 staleness 를 최소화한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCacheService {

	private final UserRepository userRepository;

	@Cacheable(
		cacheNames = CacheConfig.CACHE_USER_BY_ID,
		cacheManager = "redisCacheManager",
		key = "#userId",
		unless = "#result == null"
	)
	public UserCacheDto getById(Long userId) {
		return UserCacheDto.from(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND));
	}

	@CacheEvict(
		cacheNames = CacheConfig.CACHE_USER_BY_ID,
		cacheManager = "redisCacheManager",
		key = "#userId"
	)
	public void evict(Long userId) {
	}
}
