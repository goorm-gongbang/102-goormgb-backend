package com.goormgb.be.seat.booking.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * {@link com.goormgb.be.domain.match.entity.Match} 의 존재 여부를
 * Caffeine 로컬 캐시로 검증하는 컴포넌트.
 *
 * <p>예매 옵션 저장 API 등 "경기 존재 여부만" 필요하고 엔티티 필드를 읽지 않는
 * 지점에서 사용한다. {@code findById}를 사용하면 {@code SELECT *} 와
 * HikariCP 커넥션 점유가 요청마다 발생하지만, 본 컴포넌트는
 * {@code @Cacheable} 을 통해 DB 호출을 TTL 범위 내 1회로 제한한다.</p>
 *
 * <p>캐시 설정은 {@code application.yaml} 의 {@code spring.cache.caffeine.spec}
 * 에 정의되어 있으며, 존재하지 않는 matchId 의 결과(false)는 캐싱하지 않아
 * 매치가 생성된 직후 즉시 반영되도록 한다.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchExistenceValidator {

	private static final String CACHE_NAME = "match-exists";

	private final MatchRepository matchRepository;

	/**
	 * 주어진 {@code matchId} 의 경기가 존재하지 않으면 {@link ErrorCode#MATCH_NOT_FOUND}
	 * 예외를 던진다.
	 *
	 * @param matchId 검증할 경기 ID
	 * @throws CustomException 경기가 존재하지 않는 경우
	 */
	public void validateExists(Long matchId) {
		if (!exists(matchId)) {
			throw new CustomException(ErrorCode.MATCH_NOT_FOUND);
		}
	}

	/**
	 * 경기 존재 여부를 조회한다. 결과가 {@code true} 인 경우에만 캐시에 저장된다.
	 *
	 * @param matchId 조회할 경기 ID
	 * @return 존재하면 true
	 */
	@Cacheable(cacheNames = CACHE_NAME, key = "#matchId", unless = "!#result")
	public boolean exists(Long matchId) {
		return matchRepository.existsById(matchId);
	}
}
