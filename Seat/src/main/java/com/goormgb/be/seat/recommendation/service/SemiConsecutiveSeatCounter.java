package com.goormgb.be.seat.recommendation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;

import lombok.RequiredArgsConstructor;

/**
 * 블럭 내 "준연석(Semi-Consecutive)" 가능 묶음 수를 계산하는 컴포넌트.
 *
 * <p>준연석이란, 인접한 2개 열(row)에 걸쳐 좌석을 배치하되
 * 수평 겹침(overlap)이 존재하는 좌석 묶음을 말한다.</p>
 */
@Component
@RequiredArgsConstructor
public class SemiConsecutiveSeatCounter {

	private final SeatSegmentExtractor seatSegmentExtractor;

	/**
	 * AVAILABLE 좌석 목록에서 준연석 N석이 가능한 묶음의 총 개수를 반환한다.
	 */
	public int countSemiConsecutiveSeats(List<MatchSeat> availableSeats, int requiredSeats) {
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

	/**
	 * 세그먼트 내부가 연속 정수(base + index)인 점을 이용하여,
	 * lowerIdx 유효 범위를 산술로 직접 계산한다. O(K × N)
	 */
	private int countOverlappingCombinations(
		List<MatchSeat> upperSeg,
		List<MatchSeat> lowerSeg,
		int requiredSeats
	) {
		int count = 0;
		int upperBase = upperSeg.get(0).getTemplateColNo();
		int lowerBase = lowerSeg.get(0).getTemplateColNo();
		int upperSize = upperSeg.size();
		int lowerSize = lowerSeg.size();

		for (int upperCount = 1; upperCount < requiredSeats; upperCount++) {
			int lowerCount = requiredSeats - upperCount;

			if (upperCount > upperSize || lowerCount > lowerSize) {
				continue;
			}

			for (int ui = 0; ui <= upperSize - upperCount; ui++) {
				int liMin = Math.max(0, upperBase + ui - lowerBase - lowerCount + 1);
				int liMax = Math.min(lowerSize - lowerCount, upperBase + ui + upperCount - 1 - lowerBase);

				if (liMax >= liMin) {
					count += liMax - liMin + 1;
				}
			}
		}

		return count;
	}
}
