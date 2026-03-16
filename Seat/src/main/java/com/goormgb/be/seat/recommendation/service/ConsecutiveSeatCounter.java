package com.goormgb.be.seat.recommendation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;

import lombok.RequiredArgsConstructor;

/**
 * 블럭 내 "진짜 N연석" 가능 묶음 수를 계산하는 컴포넌트.
 *
 * <p>진짜 N연석이란, 같은 열(row) 안에서 template_col_no가 빈칸 없이 연속으로 N개 붙어있는 좌석 묶음을 말한다.</p>
 *
 * <h3>계산 방식</h3>
 * <ol>
 *   <li>해당 블럭의 AVAILABLE 좌석을 열(row)별로 그룹화한다.</li>
 *   <li>각 열에서 template_col_no가 연속인 구간(세그먼트)을 추출한다.
 *       <br>예: [1,2,3,4,5, _, 7,8,9] → 세그먼트 [1~5], [7~9]</li>
 *   <li>각 세그먼트에서 N개짜리 묶음이 몇 개 나오는지 센다.
 *       <br>공식: (세그먼트 길이 - N + 1), 음수면 0</li>
 *   <li>모든 열의 묶음 수를 합산해서 반환한다.</li>
 * </ol>
 *
 * <h3>예시</h3>
 * <pre>
 * 14칸 열에서 12석 AVAILABLE, 연속 구간이 [1~8], [10~13]이고, N=5일 때:
 *   [1~8] → 8-5+1 = 4개
 *   [10~13] → 4-5+1 = 0개 (4 < 5이므로 불가)
 *   합계 = 4개
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class ConsecutiveSeatCounter {

	private final MatchSeatRepository matchSeatRepository;

	/**
	 * 특정 경기·블럭에서 진짜 N연석이 가능한 묶음의 총 개수를 반환한다.
	 *
	 * @param matchId       경기 ID
	 * @param blockId       블럭 ID
	 * @param requiredSeats 필요 연석 수 (N)
	 * @return 진짜 N연석 가능 묶음 수 (0이면 해당 블럭에서 N연석 불가)
	 */
	public int countRealConsecutiveSeats(Long matchId, Long blockId, int requiredSeats) {
		List<MatchSeat> availableSeats = matchSeatRepository.findAvailableSeatsByMatchIdAndBlockId(matchId, blockId);

		if (availableSeats.isEmpty()) {
			return 0;
		}

		Map<Integer, List<Integer>> seatsByRow = availableSeats.stream()
			.collect(Collectors.groupingBy(
				MatchSeat::getRowNo,
				Collectors.mapping(MatchSeat::getTemplateColNo, Collectors.toList())
			));

		int totalCount = 0;

		for (List<Integer> colNos : seatsByRow.values()) {
			totalCount += countConsecutiveGroupsInRow(colNos, requiredSeats);
		}

		return totalCount;
	}

	private int countConsecutiveGroupsInRow(List<Integer> sortedColNos, int requiredSeats) {
		if (sortedColNos.size() < requiredSeats) {
			return 0;
		}

		List<List<Integer>> consecutiveSegments = extractConsecutiveSegments(sortedColNos);

		int count = 0;
		for (List<Integer> segment : consecutiveSegments) {
			if (segment.size() >= requiredSeats) {
				count += (segment.size() - requiredSeats + 1);
			}
		}

		return count;
	}

	private List<List<Integer>> extractConsecutiveSegments(List<Integer> sortedColNos) {
		List<List<Integer>> segments = new ArrayList<>();
		List<Integer> current = new ArrayList<>();
		current.add(sortedColNos.get(0));

		for (int i = 1; i < sortedColNos.size(); i++) {
			if (sortedColNos.get(i) == sortedColNos.get(i - 1) + 1) {
				current.add(sortedColNos.get(i));
			} else {
				segments.add(current);
				current = new ArrayList<>();
				current.add(sortedColNos.get(i));
			}
		}
		segments.add(current);

		return segments;
	}
}
