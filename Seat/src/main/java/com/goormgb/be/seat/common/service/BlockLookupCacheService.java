package com.goormgb.be.seat.common.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.config.CacheConfig;

import lombok.RequiredArgsConstructor;

/**
 * 블록 번호 조합을 key 로 Block(+Section+Area JOIN FETCH) 결과를 Caffeine 에 흡수.
 *
 * <p>스타디움 블록 구조는 배포 단위로만 변경되는 준정적 데이터. 유저의 선호 블록 집합은
 * 유저 간 자주 중복되므로 key 를 정규화하면 cache hit 이 잘 나온다.</p>
 *
 * <p>Key 정규화: 입력 list 의 순서/중복에 무관하게 동일 조합은 같은 key 가 되도록 정렬 + distinct.</p>
 */
@Component
@RequiredArgsConstructor
public class BlockLookupCacheService {

	private final BlockRepository blockRepository;

	/**
	 * 블록 번호 list 에 해당하는 Block 들을 Section/Area join fetch 로 조회.
	 * 캐시 key 는 정렬+distinct 된 list 의 문자열 표현.
	 */
	@Cacheable(cacheNames = CacheConfig.CACHE_BLOCKS_BY_BLOCK_NUMS, key = "#root.target.normalizeKey(#blockNums)")
	public List<Block> findAllByBlockNums(List<Long> blockNums) {
		return blockRepository.findAllByBlockNumInWithSectionAndArea(blockNums);
	}

	/**
	 * 입력 list 의 순서/중복에 무관하게 같은 조합이면 동일 key 가 되도록 정규화.
	 * @Cacheable SpEL 에서 참조할 수 있도록 public.
	 */
	public String normalizeKey(List<Long> blockNums) {
		if (blockNums == null || blockNums.isEmpty()) {
			return "empty";
		}
		List<Long> normalized = new ArrayList<>(blockNums);
		normalized.sort(Long::compareTo);
		return normalized.stream().distinct().toList().toString();
	}
}
