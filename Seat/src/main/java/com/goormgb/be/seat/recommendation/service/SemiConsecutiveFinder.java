package com.goormgb.be.seat.recommendation.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.recommendation.dto.internal.SemiGroup;

import lombok.RequiredArgsConstructor;

/**
 * 블럭 내에서 최적의 "준연석" 묶음을 탐색하는 컴포넌트.
 *
 * <p>준연석이란, 인접한 2개 열(row)에 걸쳐 좌석을 배치하되
 * 수평 겹침(overlap)이 존재하는 좌석 묶음을 말한다.</p>
 *
 * <h3>정렬 우선순위</h3>
 * <ol>
 *   <li>앞열 합 우선 (rowSum ASC)</li>
 *   <li>겹침 많음 우선 (overlapCount DESC)</li>
 *   <li>평균 통로 거리 가까움 우선 (avgAisleDistance ASC)</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class SemiConsecutiveFinder {

	private final MatchSeatRepository matchSeatRepository;
	private final AisleDistanceCalculator aisleDistanceCalculator;
	private final SeatSegmentExtractor seatSegmentExtractor;

	/**
	 * 특정 블럭에서 최적의 준연석 묶음을 찾는다.
	 *
	 * @param matchId       경기 ID
	 * @param blockId       블럭 ID
	 * @param requiredSeats 필요 좌석 수
	 * @return 최적의 준연석 그룹 (없으면 Optional.empty())
	 */
	public Optional<SemiGroup> findBestSemiConsecutive(Long matchId, Long blockId, int requiredSeats) {
		List<MatchSeat> availableSeats = matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(matchId, blockId);

		if (availableSeats.size() < requiredSeats) {
			return Optional.empty();
		}

		Map<Integer, List<MatchSeat>> seatsByRow = availableSeats.stream()
			.collect(Collectors.groupingBy(
				MatchSeat::getRowNo,
				TreeMap::new,
				Collectors.toList()
			));

		List<Integer> sortedRows = new ArrayList<>(seatsByRow.keySet());

		return IntStream.range(0, sortedRows.size() - 1)
			.boxed()
			.<SemiGroup>mapMulti((i, consumer) -> {
				int upperRow = sortedRows.get(i);
				int lowerRow = sortedRows.get(i + 1);

				if (lowerRow - upperRow != 1) {
					return;
				}

				var upperSegments = seatSegmentExtractor.extractConsecutiveSegments(seatsByRow.get(upperRow));
				var lowerSegments = seatSegmentExtractor.extractConsecutiveSegments(seatsByRow.get(lowerRow));

				for (var upperSeg : upperSegments) {
					for (var lowerSeg : lowerSegments) {
						emitCandidates(consumer, upperSeg, lowerSeg, upperRow, lowerRow, requiredSeats);
					}
				}
			})
			.min(Comparator
				.comparingInt(SemiGroup::rowSum)
				.thenComparing(Comparator.comparingInt(SemiGroup::overlapCount).reversed())
				.thenComparingInt(SemiGroup::avgAisleDistance));
	}

	/**
	 * 세그먼트 내부가 연속 정수인 점을 이용하여 overlap > 0인 lowerIdx 범위를
	 * 산술로 직접 계산하고, 해당 범위만 순회한다.
	 */
	private void emitCandidates(
		Consumer<SemiGroup> consumer,
		List<MatchSeat> upperSeg,
		List<MatchSeat> lowerSeg,
		int upperRow,
		int lowerRow,
		int requiredSeats
	) {
		int upperBase = upperSeg.get(0).getTemplateColNo();
		int lowerBase = lowerSeg.get(0).getTemplateColNo();

		for (int upperCount = 1; upperCount < requiredSeats; upperCount++) {
			int lowerCount = requiredSeats - upperCount;

			if (upperCount > upperSeg.size() || lowerCount > lowerSeg.size()) {
				continue;
			}

			for (int ui = 0; ui <= upperSeg.size() - upperCount; ui++) {
				List<MatchSeat> upperGroup = upperSeg.subList(ui, ui + upperCount);
				int upperStart = upperBase + ui;
				int upperEnd = upperBase + ui + upperCount - 1;

				// overlap > 0을 만족하는 lowerIdx 유효 범위를 산술로 계산
				int liMin = Math.max(0, upperStart - lowerBase - lowerCount + 1);
				int liMax = Math.min(lowerSeg.size() - lowerCount, upperEnd - lowerBase);

				for (int li = liMin; li <= liMax; li++) {
					List<MatchSeat> lowerGroup = lowerSeg.subList(li, li + lowerCount);
					int lowerStart = lowerBase + li;
					int lowerEnd = lowerBase + li + lowerCount - 1;

					int overlap = Math.min(upperEnd, lowerEnd) - Math.max(upperStart, lowerStart) + 1;

					int upperAisle = aisleDistanceCalculator.calculateAisleDistance(upperRow, upperStart, upperEnd);
					int lowerAisle = aisleDistanceCalculator.calculateAisleDistance(lowerRow, lowerStart, lowerEnd);

					consumer.accept(new SemiGroup(
						new ArrayList<>(upperGroup),
						new ArrayList<>(lowerGroup),
						upperRow,
						lowerRow,
						overlap,
						(upperAisle + lowerAisle) / 2
					));
				}
			}
		}
	}

}
