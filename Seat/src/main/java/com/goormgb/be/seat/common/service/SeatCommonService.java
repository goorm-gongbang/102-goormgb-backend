package com.goormgb.be.seat.common.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.common.dto.response.SeatGroupsEntryResponse;
import com.goormgb.be.seat.common.dto.response.SectionBlocksResponse;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;
import com.goormgb.be.seat.seatHold.entity.SeatHold;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.repository.SectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatCommonService {

	private final MatchRepository matchRepository;
	private final SeatPreferenceRedisRepository seatPreferenceRedisRepository;
	private final SectionRepository sectionRepository;
	private final BlockRepository blockRepository;
	private final MatchSeatRepository matchSeatRepository;
	private final SeatHoldRepository seatHoldRepository;

	@Transactional(readOnly = true)
	public SeatGroupsEntryResponse getSeatGroupsEntry(Long matchId, Long userId) {
		var match = matchRepository.findDetailByIdOrThrow(matchId);
		var seatSession = seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);

		List<Section> sections = sectionRepository.findAllWithAreaOrderByAreaIdAscSectionIdAsc();
		List<Long> sectionIds = sections.stream().map(Section::getId).toList();

		Map<Long, List<Long>> blockIdsBySectionId = createBlockIdsBySectionId(sectionIds);
		Map<Long, Long> remainingSeatCountBySectionId = createRemainingSeatCountBySectionId(matchId);

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

		return SeatGroupsEntryResponse.of(match, seatSession, seatGroups);
	}

	@Transactional(readOnly = true)
	public SectionBlocksResponse getSectionBlocks(Long matchId, Long sectionId, Long userId) {
		matchRepository.findDetailByIdOrThrow(matchId);
		seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);
		sectionRepository.findByIdOrThrow(sectionId, ErrorCode.SECTION_NOT_FOUND);

		List<Block> blocks = blockRepository.findBySectionIdOrderByBlockCodeAsc(sectionId);
		List<MatchSeat> matchSeats = matchSeatRepository.findByMatchIdAndSectionIdOrderByBlockIdAscRowNoAscSeatNoAsc(
			matchId,
			sectionId
		);

		Set<Long> sectionMatchSeatIds = matchSeats.stream()
			.map(MatchSeat::getId)
			.collect(java.util.stream.Collectors.toSet());

		Set<Long> activeHeldMatchSeatIds = seatHoldRepository
			.findAllByMatchIdAndMatchSeatIdInAndExpiresAtAfter(
				matchId,
				new ArrayList<>(sectionMatchSeatIds),
				Instant.now()
			)
			.stream()
			.map(SeatHold::getMatchSeatId)
			.collect(Collectors.toSet());

		Map<Long, BlockAccumulator> blockMap = new LinkedHashMap<>();
		for (Block block : blocks) {
			blockMap.put(block.getId(), new BlockAccumulator(block.getId(), block.getBlockCode()));
		}

		for (MatchSeat matchSeat : matchSeats) {
			BlockAccumulator block = blockMap.get(matchSeat.getBlockId());
			if (block == null) {
				continue;
			}
			RowAccumulator row = block.rowsByRowNo()
				.computeIfAbsent(matchSeat.getRowNo(), RowAccumulator::new);

			String seatSaleStatus = toSeatSaleStatus(matchSeat, activeHeldMatchSeatIds);
			row.addSeat(
				SectionBlocksResponse.SeatInfo.of(
					matchSeat.getSeatId(),
					matchSeat.getSeatNo(),
					seatSaleStatus
				)
			);

			if (MatchSeatSaleStatus.AVAILABLE.name().equals(seatSaleStatus)) {
				row.increaseRemainingSeatCount();
			}
		}

		List<SectionBlocksResponse.BlockInfo> blockInfos = blockMap.values()
			.stream()
			.map(BlockAccumulator::toResponse)
			.toList();

		return new SectionBlocksResponse(blockInfos);
	}

	private Map<Long, List<Long>> createBlockIdsBySectionId(List<Long> sectionIds) {
		if (sectionIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, List<Long>> blockIdsBySectionId = new LinkedHashMap<>();
		for (Block block : blockRepository.findBySectionIdInOrderBySectionIdAscBlockCodeAsc(sectionIds)) {
			blockIdsBySectionId.computeIfAbsent(block.getSection().getId(), ignored -> new ArrayList<>())
				.add(block.getId());
		}
		return blockIdsBySectionId;
	}

	private Map<Long, Long> createRemainingSeatCountBySectionId(Long matchId) {
		Map<Long, Long> remainingSeatCountBySectionId = new LinkedHashMap<>();
		matchSeatRepository.countRemainingSeatsByMatchIdAndSaleStatusGroupBySectionId(matchId,
				MatchSeatSaleStatus.AVAILABLE)
			.forEach(it -> remainingSeatCountBySectionId.put(it.getSectionId(), it.getRemainingSeatCount()));
		return remainingSeatCountBySectionId;
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

	private String toSeatSaleStatus(MatchSeat matchSeat, Set<Long> activeHeldMatchSeatIds) {
		if (matchSeat.getSaleStatus() == MatchSeatSaleStatus.AVAILABLE
			&& activeHeldMatchSeatIds.contains(matchSeat.getId())) {
			return "HELD";
		}
		return matchSeat.getSaleStatus().name();
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

	private record BlockAccumulator(
		Long blockId,
		String blockCode,
		Map<Integer, RowAccumulator> rowsByRowNo
	) {
		private BlockAccumulator(Long blockId, String blockCode) {
			this(blockId, blockCode, new LinkedHashMap<>());
		}

		private SectionBlocksResponse.BlockInfo toResponse() {
			return SectionBlocksResponse.BlockInfo.of(
				blockId,
				blockCode,
				rowsByRowNo.values().stream()
					.map(RowAccumulator::toResponse)
					.toList()
			);
		}
	}

	private static final class RowAccumulator {

		private final int rowNo;
		private long remainingSeatCount;
		private final List<SectionBlocksResponse.SeatInfo> seats = new ArrayList<>();

		private RowAccumulator(int rowNo) {
			this.rowNo = rowNo;
		}

		public void addSeat(SectionBlocksResponse.SeatInfo seat) {
			this.seats.add(seat);
		}

		private void increaseRemainingSeatCount() {
			this.remainingSeatCount++;
		}

		private SectionBlocksResponse.RowInfo toResponse() {
			return SectionBlocksResponse.RowInfo.of(
				rowNo,
				remainingSeatCount,
				seats
			);
		}
	}
}