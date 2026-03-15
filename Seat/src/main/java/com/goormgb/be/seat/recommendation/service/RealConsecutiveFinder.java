package com.goormgb.be.seat.recommendation.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.recommendation.dto.internal.SeatGroup;

import lombok.RequiredArgsConstructor;

/**
 * 블럭 내에서 최적의 "진짜 연석" 묶음을 탐색하는 컴포넌트.
 *
 * <p>진짜 연석이란, 같은 열(row) 안에서 templateColNo가 빈칸 없이
 * 연속으로 N개 붙어있는 좌석 묶음을 말한다.</p>
 *
 * <h3>정렬 우선순위</h3>
 * <ol>
 *   <li>앞열 우선 (rowNo ASC)</li>
 *   <li>통로 가까운 좌석 우선 (aisleDistance ASC)</li>
 *   <li>왼쪽 우선 (startCol ASC)</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class RealConsecutiveFinder {

	private final MatchSeatRepository matchSeatRepository;
	private final AisleDistanceCalculator aisleDistanceCalculator;
	private final SeatSegmentExtractor seatSegmentExtractor;

	/**
	 * 특정 블럭에서 최적의 진짜 N연석 묶음을 찾는다.
	 *
	 * @param matchId       경기 ID
	 * @param blockId       블럭 ID
	 * @param requiredSeats 필요 좌석 수
	 * @return 최적의 좌석 그룹 (없으면 Optional.empty())
	 */
	public Optional<SeatGroup> findBestRealConsecutive(Long matchId, Long blockId, int requiredSeats) {
		List<MatchSeat> availableSeats = matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(matchId, blockId);

		if (availableSeats.size() < requiredSeats) {
			return Optional.empty();
		}

		Map<Integer, List<MatchSeat>> seatsByRow = availableSeats.stream()
			.collect(Collectors.groupingBy(MatchSeat::getRowNo));

		return seatsByRow.entrySet().stream()
			.<SeatGroup>mapMulti((entry, consumer) -> {
				int rowNo = entry.getKey();
				List<MatchSeat> rowSeats = entry.getValue();

				if (rowSeats.size() < requiredSeats) {
					return;
				}

				for (List<MatchSeat> segment : seatSegmentExtractor.extractConsecutiveSegments(rowSeats)) {
					if (segment.size() < requiredSeats) {
						continue;
					}

					for (int i = 0; i <= segment.size() - requiredSeats; i++) {
						List<MatchSeat> group = segment.subList(i, i + requiredSeats);
						int startCol = group.get(0).getTemplateColNo();
						int endCol = group.get(group.size() - 1).getTemplateColNo();
						int aisleDistance = aisleDistanceCalculator.calculateAisleDistance(rowNo, startCol, endCol);

						consumer.accept(new SeatGroup(new ArrayList<>(group), rowNo, startCol, endCol, aisleDistance));
					}
				}
			})
			.min(Comparator
				.comparingInt(SeatGroup::rowNo)
				.thenComparingInt(SeatGroup::aisleDistance)
				.thenComparingInt(SeatGroup::startCol));
	}

}
