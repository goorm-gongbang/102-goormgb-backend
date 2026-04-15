package com.goormgb.be.ordercore.user.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
 * <p>프로필·온보딩 변경 등 쓰기 트랜잭션 직후에는 {@link #evictAfterCommit(Long)} 를 호출해
 * 현재 트랜잭션 커밋 완료 시점에 캐시를 무효화한다. 같은 사용자의 {@code /auth/me} 응답 캐시
 * ({@value com.goormgb.be.ordercore.config.CacheConfig#CACHE_AUTH_ME})도 동반 제거해
 * Auth-Guard 가 stale 데이터를 노출하지 않도록 보장한다.</p>
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

	/**
	 * {@code user-by-id} 와 {@code auth-me} 캐시를 모두 즉시 무효화한다.
	 *
	 * <p>키 이름 파라미터({@code #userId})는 {@link Caching} 내부 {@link CacheEvict} 의
	 * SpEL 에서 사용되므로 메서드 바디는 비어 있어도 정상 동작한다.</p>
	 */
	@Caching(evict = {
		@CacheEvict(cacheNames = CacheConfig.CACHE_USER_BY_ID, cacheManager = "redisCacheManager", key = "#userId"),
		@CacheEvict(cacheNames = CacheConfig.CACHE_AUTH_ME, cacheManager = "redisCacheManager", key = "#userId")
	})
	public void evict(Long userId) {
	}

	/**
	 * 현재 트랜잭션 커밋 성공 후에 {@link #evict(Long)} 를 호출한다.
	 *
	 * <p>쓰기 트랜잭션 중간에 evict 하면 같은 키를 읽는 동시 요청이 아직 커밋 안 된 pre-commit
	 * 스냅샷을 읽어 캐시를 재채움하는 race 가 발생할 수 있다. 이를 방지하기 위해
	 * {@link TransactionSynchronizationManager#registerSynchronization(TransactionSynchronization)}
	 * 로 {@code afterCommit} 콜백에서만 실제 evict 를 실행한다.</p>
	 *
	 * <p>활성 트랜잭션이 없는 드문 경로(예: 배치 초기화)에서 호출된 경우에는 즉시 evict 한다.</p>
	 */
	public void evictAfterCommit(Long userId) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					evict(userId);
				}
			});
		} else {
			evict(userId);
		}
	}
}
