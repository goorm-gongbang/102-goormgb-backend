package com.goormgb.be.seat.common.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.seat.config.CacheConfig;

import lombok.RequiredArgsConstructor;

/**
 * {@link Match} 의 상세 정보(홈/어웨이 클럽 + 스타디움 JOIN FETCH 포함)를 Caffeine 로컬
 * 캐시로 흡수하는 컴포넌트.
 *
 * <p>부하테스트 기준 `SELECT match ... JOIN FETCH home_club, away_club, stadium`
 * 쿼리가 매 요청마다 발생하여 DB 커넥션 풀을 소진시키는 것을 막는다. 경기 메타는
 * 거의 불변이므로 TTL 10분의 캐시 공유가 안전하다.</p>
 *
 * <p>반환되는 {@link Match} 엔티티는 **detached 상태**이므로 Lazy 연관 필드에 대해서는
 * 추가 접근이 불가능하다. 현재 호출부({@code SeatCommonService})는 JOIN FETCH 로
 * 로드된 필드만 읽으므로 안전하다.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchDetailCacheService {

	private final MatchRepository matchRepository;

	/**
	 * 경기 상세 정보를 반환한다. 캐시에 존재하면 DB 조회 없이 반환하며,
	 * 미존재 시 DB 조회 후 캐시에 저장한다. 경기가 존재하지 않을 경우
	 * {@code MATCH_NOT_FOUND} 예외가 발생하며, 예외는 캐시되지 않는다.
	 *
	 * @param matchId 경기 ID
	 * @return home/away/stadium 이 eager 로 로드된 {@link Match} (detached)
	 */
	@Cacheable(cacheNames = CacheConfig.CACHE_MATCH_DETAIL, key = "#matchId")
	public Match getDetail(Long matchId) {
		return matchRepository.findDetailByIdOrThrow(matchId);
	}
}
