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

				var upperSegments = extractConsecutiveSegments(seatsByRow.get(upperRow));
				var lowerSegments = extractConsecutiveSegments(seatsByRow.get(lowerRow));

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

	private void emitCandidates(
		Consumer<SemiGroup> consumer,
		List<MatchSeat> upperSeg,
		List<MatchSeat> lowerSeg,
		int upperRow,
		int lowerRow,
		int requiredSeats
	) {
		for (int upperCount = 1; upperCount < requiredSeats; upperCount++) {
			int lowerCount = requiredSeats - upperCount;

			if (upperCount > upperSeg.size() || lowerCount > lowerSeg.size()) {
				continue;
			}

			for (int ui = 0; ui <= upperSeg.size() - upperCount; ui++) {
				List<MatchSeat> upperGroup = upperSeg.subList(ui, ui + upperCount);
				int upperStart = upperGroup.get(0).getTemplateColNo();
				int upperEnd = upperGroup.get(upperGroup.size() - 1).getTemplateColNo();

				for (int li = 0; li <= lowerSeg.size() - lowerCount; li++) {
					List<MatchSeat> lowerGroup = lowerSeg.subList(li, li + lowerCount);
					int lowerStart = lowerGroup.get(0).getTemplateColNo();
					int lowerEnd = lowerGroup.get(lowerGroup.size() - 1).getTemplateColNo();

					int overlap = Math.min(upperEnd, lowerEnd) - Math.max(upperStart, lowerStart) + 1;

					if (overlap <= 0) {
						continue;
					}

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

	private List<List<MatchSeat>> extractConsecutiveSegments(List<MatchSeat> sortedSeats) {
		List<List<MatchSeat>> segments = new ArrayList<>();
		List<MatchSeat> current = new ArrayList<>();
		current.add(sortedSeats.get(0));

		for (int i = 1; i < sortedSeats.size(); i++) {
			if (sortedSeats.get(i).getTemplateColNo() == sortedSeats.get(i - 1).getTemplateColNo() + 1) {
				current.add(sortedSeats.get(i));
			} else {
				segments.add(current);
				current = new ArrayList<>();
				current.add(sortedSeats.get(i));
			}
		}
		segments.add(current);

		return segments;
	}
}
