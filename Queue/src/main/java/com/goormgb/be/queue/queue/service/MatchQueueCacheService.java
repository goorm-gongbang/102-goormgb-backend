package com.goormgb.be.queue.queue.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.queue.config.CacheConfig;

import lombok.RequiredArgsConstructor;

/**
 * 대기열 진입 검증용 {@link Match} 조회를 Caffeine 로컬 캐시로 흡수하는 컴포넌트.
 *
 * <p>Queue 서비스는 대기열 진입 시점마다 {@code findByIdOrThrow} 로 경기 존재 여부와
 * {@code saleStatus} 를 확인한다. 이 쿼리는 부하 집중 시 DB 커넥션을 빠르게
 * 소진시키므로 로컬 캐시로 차단한다.</p>
 *
 * <p>{@code saleStatus} 가 전환되는 티켓팅 오픈 직전/직후에 캐시 stale 데이터로 인한
 * 판단 오류가 발생하지 않도록 TTL 은 1분으로 짧게 설정한다. 최악의 경우에도 캐시
 * 만료 전까지 1분간 과거 상태가 보일 수 있음을 수용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchQueueCacheService {

	private final MatchRepository matchRepository;

	/**
	 * 경기 정보를 반환한다. 캐시에 존재하면 DB 조회 없이 반환하며, 존재하지 않으면
	 * {@link ErrorCode#MATCH_NOT_FOUND} 예외가 발생하고 예외는 캐시되지 않는다.
	 *
	 * @param matchId 조회할 경기 ID
	 * @return {@link Match} (detached 상태이며 lazy 필드 접근 금지)
	 */
	@Cacheable(cacheNames = CacheConfig.CACHE_MATCH_FOR_QUEUE, key = "#matchId")
	public Match getForQueue(Long matchId) {
		return matchRepository.findByIdOrThrow(matchId, ErrorCode.MATCH_NOT_FOUND);
	}
}
