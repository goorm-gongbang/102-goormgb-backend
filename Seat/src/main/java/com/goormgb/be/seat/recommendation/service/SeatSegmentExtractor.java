package com.goormgb.be.seat.recommendation.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;

/**
 * templateColNo 기준 연속 좌석 구간을 추출하는 공통 컴포넌트.
 *
 * <p>RealConsecutiveFinder, SemiConsecutiveFinder에서 공통으로 사용한다.</p>
 */
@Component
public class SeatSegmentExtractor {

	public List<List<MatchSeat>> extractConsecutiveSegments(List<MatchSeat> sortedSeats) {
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
