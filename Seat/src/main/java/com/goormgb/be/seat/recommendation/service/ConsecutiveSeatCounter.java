package com.goormgb.be.seat.recommendation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;

/**
 * 블럭 내 "진짜 N연석" 가능 묶음 수를 계산하는 컴포넌트.
 *
 * <p>같은 열(row) 안에서 template_col_no가 빈칸 없이 연속으로 N개 붙어있는 좌석 묶음을 센다.</p>
 */
@Component
public class ConsecutiveSeatCounter {

	/**
	 * AVAILABLE 좌석 목록에서 진짜 N연석이 가능한 묶음의 총 개수를 반환한다.
	 */
	public int countRealConsecutiveSeats(List<MatchSeat> availableSeats, int requiredSeats) {
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
