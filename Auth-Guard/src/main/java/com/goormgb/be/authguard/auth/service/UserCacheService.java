package com.goormgb.be.authguard.auth.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.authguard.config.CacheConfig;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.user.dto.cache.UserCacheDto;
import com.goormgb.be.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Auth-Guard 에서 사용자 정보를 Redis 분산 캐시를 경유해 조회하는 서비스.
 *
 * <p>매 요청마다 발생하던 {@code UserRepository.findByIdOrThrow} 호출을 흡수해 DB 부하를 줄인다.
 * Pod 간 즉시 전파가 필요하므로 로컬 캐시가 아닌 Redis 를 사용한다 (Phase 2 설계).</p>
 *
 * <p>캐시 TTL 은 10분이며, 닉네임·상태 변경 같은 프로필 수정 지점에서
 * {@link #evict(Long)} 을 호출하거나 {@code @CacheEvict} 를 걸어 즉시 무효화한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCacheService {

	private final UserRepository userRepository;

	/**
	 * userId 로 {@link UserCacheDto} 를 반환한다. 캐시 미스 시 DB 에서 조회해 캐시에 저장한다.
	 *
	 * @param userId 대상 사용자 ID
	 * @return 사용자 스냅샷 DTO
	 */
	@Cacheable(cacheNames = CacheConfig.CACHE_USER_BY_ID, key = "#userId", unless = "#result == null")
	public UserCacheDto getById(Long userId) {
		return UserCacheDto.from(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND));
	}

	/**
	 * 해당 사용자 캐시를 즉시 무효화한다. 닉네임·상태·프로필 변경 직후 호출한다.
	 */
	@CacheEvict(cacheNames = CacheConfig.CACHE_USER_BY_ID, key = "#userId")
	public void evict(Long userId) {
	}
}
