package com.goormgb.be.seat.common.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.common.dto.cache.SeatGroupsCachePayload;
import com.goormgb.be.seat.common.dto.response.SeatGroupsEntryResponse;
import com.goormgb.be.seat.config.CacheConfig;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.section.entity.Section;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code GET /matches/{matchId}/seat-groups} 응답의 유저 독립 payload 를 Redis 에 캐싱하는 서비스 (Phase 3).
 *
 * <p>Phase 1 에서 Match / Section / Block 은 Caffeine 로컬 캐시로 흡수됐으나,
 * {@code countRemainingSeatsByMatchIdAndSaleStatusGroupBySectionId} 집계 쿼리는 여전히 매 요청 DB 를 때렸다.
 * 본 서비스는 응답 조립 결과를 통째로 Redis 에 TTL 5초로 캐시해 고빈도 호출의 DB·JVM 연산 비용을 동시에 제거한다.</p>
 *
 * <p>유저별 {@code seatSession} 은 본 payload 에서 배제하며, 최종 응답은
 * {@code SeatCommonService#getSeatGroupsEntry} 에서 유저 정보를 덧씌워 조립한다.</p>
 *
 * <p>짧은 TTL(5s) 로 좌석 선점·해제로 인한 잔여 좌석 수 stale 을 최소화한다. 3~5초 지연은 UX 상 감지되지 않는 수준이다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatGroupsResponseCacheService {

	private final MatchDetailCacheService matchDetailCacheService;
	private final SectionLookupCacheService sectionLookupCacheService;
	private final MatchSeatRepository matchSeatRepository;

	/**
	 * matchId 기준 유저 독립 payload 를 반환한다. 캐시 미스 시 DB 에서 재계산 후 Redis 에 저장한다.
	 *
	 * @param matchId 경기 ID
	 * @return 캐시 가능한 응답 조각
	 */
	@Cacheable(
		cacheNames = CacheConfig.CACHE_SEAT_GROUPS_RESPONSE,
		cacheManager = "redisCacheManager",
		key = "#matchId",
		unless = "#result == null"
	)
	@Transactional(readOnly = true)
	public SeatGroupsCachePayload getPayload(Long matchId) {
		long start = System.currentTimeMillis();

		Match match = matchDetailCacheService.getDetail(matchId);
		List<Section> sections = sectionLookupCacheService.findAllSectionsWithArea();
		List<Long> sectionIds = sections.stream().map(Section::getId).toList();

		Map<Long, List<Long>> blockIdsBySectionId = buildBlockIdsBySectionId(sectionIds);
		Map<Long, Long> remainingSeatCountBySectionId = buildRemainingSeatCountBySectionId(matchId);

		Map<Long, SeatGroupAccumulator> groupMap = new LinkedHashMap<>();
		for (Section section : sections) {
			Long areaId = section.getArea().getId();
			SeatGroupAccumulator group = groupMap.computeIfAbsent(areaId,
				ignored -> new SeatGroupAccumulator(areaId, toAreaName(section.getArea().getCode())));

			group.sections().add(new SeatGroupsEntryResponse.SectionInfo(
				section.getId(),
				section.getCode().name(),
				buildDisplayName(section),
				blockIdsBySectionId.getOrDefault(section.getId(), List.of()),
				remainingSeatCountBySectionId.getOrDefault(section.getId(), 0L)
			));
		}

		List<SeatGroupsEntryResponse.SeatGroupInfo> seatGroups = groupMap.values()
			.stream()
			.map(it -> new SeatGroupsEntryResponse.SeatGroupInfo(it.areaId(), it.areaName(), it.sections()))
			.toList();

		log.info("[SeatGroupsResponseCacheService#getPayload] MISS computed - at={}, matchId={}, elapsed={}ms",
			LocalDateTime.now(ZoneId.of("Asia/Seoul")), matchId, System.currentTimeMillis() - start);

		return new SeatGroupsCachePayload(SeatGroupsEntryResponse.MatchInfo.from(match), seatGroups);
	}

	private Map<Long, List<Long>> buildBlockIdsBySectionId(List<Long> sectionIds) {
		if (sectionIds.isEmpty()) {
			return Map.of();
		}
		List<Block> blocks = sectionLookupCacheService.findBlocksBySectionIds(sectionIds);
		Map<Long, List<Long>> result = new LinkedHashMap<>();
		for (Block block : blocks) {
			result.computeIfAbsent(block.getSection().getId(), ignored -> new ArrayList<>())
				.add(block.getBlockNum());
		}
		return result;
	}

	private Map<Long, Long> buildRemainingSeatCountBySectionId(Long matchId) {
		Map<Long, Long> result = new LinkedHashMap<>();
		matchSeatRepository.countRemainingSeatsByMatchIdAndSaleStatusGroupBySectionId(matchId,
				MatchSeatSaleStatus.AVAILABLE)
			.forEach(it -> result.put(it.getSectionId(), it.getRemainingSeatCount()));
		return result;
	}

	private String buildDisplayName(Section section) {
		return switch (section.getArea().getCode()) {
			case HOME -> "1루 " + section.getName();
			case AWAY -> "3루 " + section.getName();
			default -> section.getName();
		};
	}

	private String toAreaName(AreaCode areaCode) {
		return switch (areaCode) {
			case CENTER -> "프리미엄";
			case HOME -> "1루 구역";
			case AWAY -> "3루 구역";
			case OUTFIELD -> "외야 구역";
		};
	}

	private record SeatGroupAccumulator(
		Long areaId,
		String areaName,
		List<SeatGroupsEntryResponse.SectionInfo> sections
	) {
		private SeatGroupAccumulator(Long areaId, String areaName) {
			this(areaId, areaName, new ArrayList<>());
		}
	}
}
