package com.goormgb.be.ordercore.match.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.ordercore.config.CacheConfig;

import lombok.RequiredArgsConstructor;

/**
 * 주문서 조회 등 <b>읽기 전용</b> 경로에서 사용하는 {@link Match} 상세 조회 캐시.
 *
 * <p>반환되는 엔티티는 영속성 컨텍스트와 무관한 <b>detached</b> 상태이므로 JPA 저장
 * 연산(e.g. {@code Order.match} 로 연결해 persist) 에는 사용해선 안 된다. 현재 호출부인
 * {@code OrderService#getOrderSheet} 는 응답 조립용으로만 쓰므로 안전하다.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchDetailCacheService {

	private final MatchRepository matchRepository;

	/**
	 * 경기 상세 정보를 반환한다.
	 *
	 * @param matchId 경기 ID
	 * @return home/away/stadium 이 eager 로 로드된 detached {@link Match}
	 */
	@Cacheable(cacheNames = CacheConfig.CACHE_MATCH_DETAIL, key = "#matchId")
	public Match getDetail(Long matchId) {
		return matchRepository.findDetailByIdOrThrow(matchId);
	}
}
