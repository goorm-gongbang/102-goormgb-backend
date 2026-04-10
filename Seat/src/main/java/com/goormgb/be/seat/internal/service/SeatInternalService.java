package com.goormgb.be.seat.internal.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.ticket.enums.TicketType;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.internal.dto.PriceResponse;
import com.goormgb.be.seat.internal.dto.SeatHoldInfoResponse;
import com.goormgb.be.seat.internal.dto.SectionBlockInfoResponse;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.pricePolicy.entity.PricePolicy;
import com.goormgb.be.seat.pricePolicy.enums.DayType;
import com.goormgb.be.seat.pricePolicy.repository.PricePolicyRepository;
import com.goormgb.be.seat.seatHold.entity.SeatHold;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.repository.SectionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Order-Core 모듈에서 호출하는 내부 API용 서비스.
 * DB 스키마 분리 시 Order-Core가 Seat DB를 직접 조회하지 않도록 중간 계층 역할을 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatInternalService {

	private final SeatHoldRepository seatHoldRepository;
	private final MatchSeatRepository matchSeatRepository;
	private final PricePolicyRepository pricePolicyRepository;
	private final SectionRepository sectionRepository;
	private final BlockRepository blockRepository;

	/**
	 * matchSeatIds에 해당하는 좌석 선점 정보를 조회한다.
	 */
	public List<SeatHoldInfoResponse> findSeatHoldInfos(Long userId, Long matchId, List<Long> matchSeatIds) {
		Instant now = Instant.now();
		List<SeatHold> holds = seatHoldRepository.findAllByMatchIdAndMatchSeatIdInAndExpiresAtAfter(
			matchId, matchSeatIds, now);

		if (holds.isEmpty()) {
			return List.of();
		}

		// matchSeatId → MatchSeat 매핑
		List<MatchSeat> matchSeats = matchSeatRepository.findAllById(matchSeatIds);
		Map<Long, MatchSeat> matchSeatMap = matchSeats.stream()
			.collect(Collectors.toMap(MatchSeat::getId, Function.identity()));

		// sectionId, blockId 수집
		Set<Long> sectionIds = matchSeats.stream().map(MatchSeat::getSectionId).collect(Collectors.toSet());
		Set<Long> blockIds = matchSeats.stream().map(MatchSeat::getBlockId).collect(Collectors.toSet());

		Map<Long, Section> sectionMap = sectionRepository.findAllById(sectionIds).stream()
			.collect(Collectors.toMap(Section::getId, Function.identity()));
		Map<Long, Block> blockMap = blockRepository.findAllById(blockIds).stream()
			.collect(Collectors.toMap(Block::getId, Function.identity()));

		return holds.stream()
			.filter(h -> h.getUserId().equals(userId))
			.map(hold -> {
				MatchSeat ms = matchSeatMap.get(hold.getMatchSeatId());
				Section section = ms != null ? sectionMap.get(ms.getSectionId()) : null;
				Block block = ms != null ? blockMap.get(ms.getBlockId()) : null;
				return new SeatHoldInfoResponse(
					hold.getId(),
					hold.getMatchSeatId(),
					hold.getUserId(),
					hold.getExpiresAt(),
					ms != null ? ms.getSectionId() : null,
					section != null ? section.getName() : null,
					ms != null ? ms.getBlockId() : null,
					block != null ? block.getBlockCode() : null,
					ms != null ? ms.getRowNo() : null,
					ms != null ? ms.getSeatNo() : null
				);
			})
			.toList();
	}

	/**
	 * 구역/요일/좌석유형 조합에 해당하는 가격을 조회한다.
	 */
	public PriceResponse findPrice(Long sectionId, String dayType, String ticketType) {
		DayType day = DayType.valueOf(dayType);
		TicketType ticket = TicketType.valueOf(ticketType);

		return pricePolicyRepository.findBySectionIdAndDayTypeAndTicketType(sectionId, day, ticket)
			.map(pp -> new PriceResponse(pp.getPrice()))
			.orElse(new PriceResponse(null));
	}

	/**
	 * sectionId, blockId 목록에 대한 이름 정보를 반환한다.
	 */
	public List<SectionBlockInfoResponse> findSectionBlockInfos(List<Long> sectionIds, List<Long> blockIds) {
		Map<Long, Section> sectionMap = sectionRepository.findAllById(sectionIds).stream()
			.collect(Collectors.toMap(Section::getId, Function.identity()));
		Map<Long, Block> blockMap = blockRepository.findAllById(blockIds).stream()
			.collect(Collectors.toMap(Block::getId, Function.identity()));

		return blockMap.values().stream()
			.map(block -> {
				Section section = sectionMap.get(block.getSection().getId());
				return new SectionBlockInfoResponse(
					section != null ? section.getId() : null,
					section != null ? section.getName() : null,
					block.getId(),
					block.getBlockCode()
				);
			})
			.toList();
	}
}
