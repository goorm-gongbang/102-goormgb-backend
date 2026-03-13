package com.goormgb.be.seat.matchSeat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.seat.dto.SeatTemplateProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchSeatPreparationTransactionalService {

	private static final int BATCH_SIZE = 1000;

	private final MatchSeatRepository matchSeatRepository;

	@Transactional
	public boolean prepareSingleMatchSeats(Long matchId, List<SeatTemplateProjection> templates) {
		if (matchSeatRepository.existsByMatchId(matchId)) {
			return false;
		}

		for (int start = 0; start < templates.size(); start += BATCH_SIZE) {
			int end = Math.min(start + BATCH_SIZE, templates.size());

			List<MatchSeat> batch = templates.subList(start, end)
				.stream()
				.map(template -> MatchSeat.builder()
					.matchId(matchId)
					.seatId(template.getSeatId())
					.areaId(template.getAreaId())
					.sectionId(template.getSectionId())
					.blockId(template.getBlockId())
					.rowNo(template.getRowNo())
					.seatNo(template.getSeatNo())
					.templateColNo(template.getTemplateColNo())
					.seatZone(template.getSeatZone())
					.saleStatus(MatchSeatSaleStatus.AVAILABLE)
					.build())
				.toList();

			matchSeatRepository.saveAll(batch);
		}

		return true;
	}
}
