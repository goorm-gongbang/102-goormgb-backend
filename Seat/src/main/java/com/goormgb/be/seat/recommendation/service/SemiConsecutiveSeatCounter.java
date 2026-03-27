package com.goormgb.be.seat.recommendation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;

import lombok.RequiredArgsConstructor;

/**
 * 블럭 내 "준연석(Semi-Consecutive)" 가능 묶음 수를 계산하는 컴포넌트.
 *
 * <p>준연석이란, 인접한 2개 열(row)에 걸쳐 좌석을 배치하되
 * 수평 겹침(overlap)이 존재하는 좌석 묶음을 말한다.</p>
 *
 * <h3>계산 방식</h3>
 * <ol>
 *   <li>해당 블럭의 AVAILABLE 좌석을 열(row)별로 그룹화한다.</li>
 *   <li>인접한 row 쌍(lowerRow - upperRow == 1)에 대해 연속 세그먼트를 추출한다.</li>
 *   <li>각 (upperSeg, lowerSeg) 조합에서 N석을 upper/lower로 분배하는 모든 경우를 탐색한다.</li>
 *   <li>수평 겹침(overlap)이 존재하는 유효한 조합만 카운트한다.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class SemiConsecutiveSeatCounter {

	private final MatchSeatRepository matchSeatRepository;
	private final SeatSegmentExtractor seatSegmentExtractor;

	/**
	 * 특정 경기·블럭에서 준연석 N석이 가능한 묶음의 총 개수를 반환한다.
	 *
	 * @param matchId       경기 ID
	 * @param blockId       블럭 ID
	 * @param requiredSeats 필요 좌석 수 (N)
	 * @return 준연석 N석 가능 묶음 수 (0이면 해당 블럭에서 준연석 불가)
	 */
	public int countSemiConsecutiveSeats(Long matchId, Long blockId, int requiredSeats) {
		List<MatchSeat> availableSeats = matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(matchId, blockId);

		if (availableSeats.size() < requiredSeats) {
			return 0;
		}

		Map<Integer, List<MatchSeat>> seatsByRow = availableSeats.stream()
			.collect(Collectors.groupingBy(
				MatchSeat::getRowNo,
				TreeMap::new,
				Collectors.toList()
			));

		List<Integer> sortedRows = new ArrayList<>(seatsByRow.keySet());
		int totalCount = 0;

		for (int i = 0; i < sortedRows.size() - 1; i++) {
			int upperRow = sortedRows.get(i);
			int lowerRow = sortedRows.get(i + 1);

			if (lowerRow - upperRow != 1) {
				continue;
			}

			var upperSegments = seatSegmentExtractor.extractConsecutiveSegments(seatsByRow.get(upperRow));
			var lowerSegments = seatSegmentExtractor.extractConsecutiveSegments(seatsByRow.get(lowerRow));

			for (var upperSeg : upperSegments) {
				for (var lowerSeg : lowerSegments) {
					totalCount += countOverlappingCombinations(upperSeg, lowerSeg, requiredSeats);
				}
			}
		}

		return totalCount;
	}

	private int countOverlappingCombinations(
		List<MatchSeat> upperSeg,
		List<MatchSeat> lowerSeg,
		int requiredSeats
	) {
		int count = 0;

		for (int upperCount = 1; upperCount < requiredSeats; upperCount++) {
			int lowerCount = requiredSeats - upperCount;

			if (upperCount > upperSeg.size() || lowerCount > lowerSeg.size()) {
				continue;
			}

			for (int upperIdx = 0; upperIdx <= upperSeg.size() - upperCount; upperIdx++) {
				int upperStart = upperSeg.get(upperIdx).getTemplateColNo();
				int upperEnd = upperSeg.get(upperIdx + upperCount - 1).getTemplateColNo();

				for (int lowerIdx = 0; lowerIdx <= lowerSeg.size() - lowerCount; lowerIdx++) {
					int lowerStart = lowerSeg.get(lowerIdx).getTemplateColNo();
					int lowerEnd = lowerSeg.get(lowerIdx + lowerCount - 1).getTemplateColNo();

					int overlap = Math.min(upperEnd, lowerEnd) - Math.max(upperStart, lowerStart) + 1;

					if (overlap > 0) {
						count++;
					}
				}
			}
		}

		return count;
	}
}
