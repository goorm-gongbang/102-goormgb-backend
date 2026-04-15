package com.goormgb.be.seat.common.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.common.dto.cache.SeatGroupsCachePayload;
import com.goormgb.be.seat.common.dto.response.SeatGroupsEntryResponse;
import com.goormgb.be.seat.common.dto.response.SectionBlocksResponse;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.booking.repository.BookingOptionsRedisRepository;
import com.goormgb.be.seat.redis.SeatSession;
import com.goormgb.be.seat.seatHold.entity.SeatHold;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;
import com.goormgb.be.seat.section.repository.SectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatCommonService {

	private final MatchDetailCacheService matchDetailCacheService;
	private final SeatGroupsResponseCacheService seatGroupsResponseCacheService;
	private final BookingOptionsRedisRepository bookingOptionsRedisRepository;
	private final SectionRepository sectionRepository;
	private final BlockRepository blockRepository;
	private final MatchSeatRepository matchSeatRepository;
	private final SeatHoldRepository seatHoldRepository;

	/**
	 * 좌석 선택 페이지 진입용 응답을 조립한다 (Phase 3 Redis 응답 캐시 적용).
	 *
	 * <p>유저에 독립적인 {@code match + seatGroups} 부분은
	 * {@link SeatGroupsResponseCacheService#getPayload(Long)} 에서 Redis (TTL 5초) 로 제공된다.
	 * 유저별 {@code seatSession} 은 매 요청 booking-options(Redis) 에서 조립한다.</p>
	 *
	 * <p>트랜잭션은 의도적으로 선언하지 않는다. 캐시 히트 시 DB 커넥션을 점유하지 않도록
	 * 실제 DB 조회가 발생하는 {@code getPayload} 내부에서만 {@link Transactional} 이 열리도록 한다.</p>
	 */
	public SeatGroupsEntryResponse getSeatGroupsEntry(Long matchId, Long userId) {
		long totalStart = System.currentTimeMillis();

		SeatGroupsCachePayload payload = seatGroupsResponseCacheService.getPayload(matchId);
		var bookingOptions = bookingOptionsRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);
		var seatSession = SeatSession.from(bookingOptions);

		log.info("[SeatCommonService#getSeatGroupsEntry] at={}, matchId={}, total={}ms",
			LocalDateTime.now(ZoneId.of("Asia/Seoul")), matchId, System.currentTimeMillis() - totalStart);

		return new SeatGroupsEntryResponse(
			payload.match(),
			SeatGroupsEntryResponse.SeatSessionInfo.from(seatSession),
			payload.seatGroups()
		);
	}

	@Transactional(readOnly = true)
	public SectionBlocksResponse getSectionBlocks(Long matchId, Long sectionId, Long userId) {
		matchDetailCacheService.getDetail(matchId);
		bookingOptionsRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId);
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
			blockMap.put(block.getId(), new BlockAccumulator(block.getBlockNum(), block.getBlockCode()));
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

	private String toSeatSaleStatus(MatchSeat matchSeat, Set<Long> activeHeldMatchSeatIds) {
		if (matchSeat.getSaleStatus() == MatchSeatSaleStatus.AVAILABLE
			&& activeHeldMatchSeatIds.contains(matchSeat.getId())) {
			return "HELD";
		}
		return matchSeat.getSaleStatus().name();
	}

	private record BlockAccumulator(
		Long blockNum,
		String blockCode,
		Map<Integer, RowAccumulator> rowsByRowNo
	) {
		private BlockAccumulator(Long blockNum, String blockCode) {
			this(blockNum, blockCode, new LinkedHashMap<>());
		}

		private SectionBlocksResponse.BlockInfo toResponse() {
			return SectionBlocksResponse.BlockInfo.of(
				blockNum,
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