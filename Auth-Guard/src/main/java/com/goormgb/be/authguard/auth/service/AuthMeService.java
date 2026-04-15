package com.goormgb.be.authguard.auth.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.authguard.config.CacheConfig;
import com.goormgb.be.user.dto.cache.UserCacheDto;
import com.goormgb.be.user.dto.response.UserInfoGetResponse;

import lombok.RequiredArgsConstructor;

/**
 * {@code GET /me} 엔드포인트의 응답을 Redis 분산 캐시로 흡수하는 서비스.
 *
 * <p>프론트가 주기적으로 호출하는 확인 API 로 매 요청마다 DB 를 때리던 구간이다.
 * {@link UserCacheService} 가 제공하는 사용자 스냅샷을 기반으로 응답 DTO 를 조립한 뒤,
 * 30초 TTL 로 캐시한다. 30초 지연은 로그인 직후 프로필 일관성 측면에서 수용 가능한 수준이다.</p>
 *
 * <p>닉네임 변경·차단/해제·탈퇴 등 사용자 상태가 바뀌는 지점에서는
 * {@link #evict(Long)} 로 즉시 무효화해 Pod 간 전파를 보장한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthMeService {

	private final UserCacheService userCacheService;

	/**
	 * 로그인한 사용자의 기본 정보를 반환한다. Redis 캐시 미스 시 {@link UserCacheService} 를
	 * 거쳐 사용자 스냅샷을 조회하고, 응답 DTO 를 조립해 캐시에 저장한다.
	 */
	@Cacheable(cacheNames = CacheConfig.CACHE_AUTH_ME, key = "#userId", unless = "#result == null")
	public UserInfoGetResponse getMyInfo(Long userId) {
		UserCacheDto user = userCacheService.getById(userId);
		boolean onboardingRequired = !Boolean.TRUE.equals(user.onboardingCompleted());
		return new UserInfoGetResponse(
			user.id(),
			user.status(),
			user.email(),
			user.nickname(),
			onboardingRequired
		);
	}

	/**
	 * 해당 사용자의 {@code /me} 응답 캐시를 무효화한다. 사용자 상태 변경 직후 호출한다.
	 */
	@CacheEvict(cacheNames = CacheConfig.CACHE_AUTH_ME, key = "#userId")
	public void evict(Long userId) {
	}
}
