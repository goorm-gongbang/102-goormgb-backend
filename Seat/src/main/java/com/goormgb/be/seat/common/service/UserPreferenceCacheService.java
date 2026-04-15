package com.goormgb.be.seat.common.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferenceRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferredBlockRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingViewpointPriorityRepository;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.config.CacheConfig;

import lombok.RequiredArgsConstructor;

/**
 * 유저 온보딩 설정(선호 블록, 좌석 선호, 시야 우선순위)을 Caffeine 로컬 캐시로 흡수.
 *
 * <p>/recommendations/blocks 엔드포인트가 요청마다 아래 3개 DB 조회를 발생시켜
 * DB 부하의 상당수를 차지했다. 온보딩 설정은 유저가 직접 재설정하지 않는 한
 * 변하지 않으므로 TTL 10분 로컬 캐시로 흡수해도 정합성 문제가 없다.</p>
 *
 * <p>변경 경로(온보딩 수정 API 등)에서는 별도로 evict 하거나, TTL 경과를 기다린다.</p>
 */
@Component
@RequiredArgsConstructor
public class UserPreferenceCacheService {

	private final OnboardingPreferenceRepository onboardingPreferenceRepository;
	private final OnboardingPreferredBlockRepository onboardingPreferredBlockRepository;
	private final OnboardingViewpointPriorityRepository onboardingViewpointPriorityRepository;

	/**
	 * 유저의 선호 블록 ID 목록을 반환한다.
	 */
	@Cacheable(cacheNames = CacheConfig.CACHE_USER_PREFERRED_BLOCKS, key = "#userId")
	public List<Long> getPreferredBlockIds(Long userId) {
		return onboardingPreferredBlockRepository.findBlockIdsByUserId(userId);
	}

	/**
	 * 유저의 좌석 선호 설정을 반환한다. 미존재 시 {@link ErrorCode#PREFERENCE_NOT_FOUND}.
	 */
	@Cacheable(cacheNames = CacheConfig.CACHE_USER_PREFERENCE, key = "#userId")
	public OnboardingPreference getPreference(Long userId) {
		return onboardingPreferenceRepository.findByUserIdOrThrow(
			userId, ErrorCode.PREFERENCE_NOT_FOUND);
	}

	/**
	 * 유저의 시야 우선순위 목록을 반환한다 (우선순위 오름차순).
	 */
	@Cacheable(cacheNames = CacheConfig.CACHE_USER_VIEWPOINT_PRIORITY, key = "#userId")
	public List<OnboardingViewpointPriority> getViewpointPriorities(Long userId) {
		return onboardingViewpointPriorityRepository.findAllByUserIdOrderByPriorityAsc(userId);
	}
}
