package com.goormgb.be.seat.common.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.config.CacheConfig;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.repository.SectionRepository;

import lombok.RequiredArgsConstructor;

/**
 * 스타디움 구조(섹션/블록)와 같이 배포 단위로만 변경되는 정적 데이터 조회를 Caffeine
 * 로컬 캐시로 흡수하는 컴포넌트.
 *
 * <p>섹션·블록은 운영 중 변경되지 않는 준불변(near-immutable) 데이터임에도 매 좌석 진입
 * 요청마다 DB 조회가 발생해 커넥션 풀을 소진시키는 원인이 되었다. TTL 1시간의 Caffeine
 * 캐시로 흡수하여 DB 부하를 제거한다.</p>
 *
 * <p>캐시에 담기는 엔티티는 detached 상태이므로 JOIN FETCH 로 이미 로드된 연관 객체
 * ({@code Section.area}, {@code Block.section})만 접근해야 한다.</p>
 */
@Component
@RequiredArgsConstructor
public class SectionLookupCacheService {

	private static final String KEY_ALL_SECTIONS = "'all'";

	private final SectionRepository sectionRepository;
	private final BlockRepository blockRepository;

	/**
	 * 모든 섹션을 area eager fetch 포함하여 반환한다. 스타디움당 섹션 수가 제한적이므로
	 * 단일 엔트리로 캐싱한다.
	 *
	 * @return area 가 로드된 {@link Section} 리스트
	 */
	@Cacheable(cacheNames = CacheConfig.CACHE_SECTION_ALL, key = KEY_ALL_SECTIONS)
	@Transactional(readOnly = true)
	public List<Section> findAllSectionsWithArea() {
		return sectionRepository.findAllWithAreaOrderByAreaIdAscSectionIdAsc();
	}

	/**
	 * 주어진 섹션 ID 집합에 속한 블록 목록을 반환한다. 각 블록의 {@code section} 연관은
	 * 이미 로드된 상태이므로 호출측에서 추가 쿼리 없이 사용할 수 있다.
	 *
	 * <p>캐시 키는 정렬된 sectionIds 문자열 조합으로 생성해 호출 순서에 따른 중복 캐시
	 * 엔트리를 방지한다.</p>
	 *
	 * @param sectionIds 조회할 섹션 ID 리스트
	 * @return 블록 리스트 (빈 입력 시 빈 리스트)
	 */
	@Cacheable(
		cacheNames = CacheConfig.CACHE_BLOCKS_BY_SECTION_IDS,
		key = "T(com.goormgb.be.seat.common.service.SectionLookupCacheService).buildSectionIdsKey(#sectionIds)",
		unless = "#result.isEmpty()"
	)
	@Transactional(readOnly = true)
	public List<Block> findBlocksBySectionIds(List<Long> sectionIds) {
		if (sectionIds == null || sectionIds.isEmpty()) {
			return List.of();
		}
		return blockRepository.findBySectionIdInWithSectionOrderBySectionIdAscBlockCodeAsc(sectionIds);
	}

	/**
	 * 캐시 키 생성 유틸. 리스트의 {@code hashCode()} 는 원소 순서에 민감하고 서로 다른
	 * 리스트가 같은 해시를 가질 수 있어 안정적이지 않으므로, 정렬된 문자열로 결합한다.
	 */
	public static String buildSectionIdsKey(List<Long> sectionIds) {
		if (sectionIds == null || sectionIds.isEmpty()) {
			return "empty";
		}
		return sectionIds.stream()
			.sorted()
			.map(String::valueOf)
			.reduce((a, b) -> a + "," + b)
			.orElse("empty");
	}
}
