package com.goormgb.be.seat.recommendation.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.goormgb.be.seat.config.RowPattern;
import com.goormgb.be.seat.config.SeatLayoutPatterns;

/**
 * 좌석 그룹의 통로 거리를 계산하는 컴포넌트.
 *
 * <p>통로 거리란, 좌석 그룹의 양쪽 끝에서 가장 가까운 통로까지의 좌석 수를 말한다.
 * 값이 작을수록 통로에 가까운 좌석이다.</p>
 *
 * <h3>계산 방식</h3>
 * <ol>
 *   <li>해당 row의 레이아웃 패턴에서 좌석 시작 위치와 좌석 수를 조회한다.</li>
 *   <li>좌석 그룹의 왼쪽 끝에서 왼쪽 통로까지의 거리를 계산한다.</li>
 *   <li>좌석 그룹의 오른쪽 끝에서 오른쪽 통로까지의 거리를 계산한다.</li>
 *   <li>둘 중 작은 값을 반환한다.</li>
 * </ol>
 */
@Component
public class AisleDistanceCalculator {

	private static final List<RowPattern> LAYOUT = SeatLayoutPatterns.STANDARD;

	/**
	 * 좌석 그룹의 통로까지 최소 거리를 계산한다.
	 *
	 * @param rowNo          열 번호 (1-based)
	 * @param groupStartCol  그룹의 시작 templateColNo
	 * @param groupEndCol    그룹의 끝 templateColNo
	 * @return 통로까지의 최소 거리 (0 = 통로 바로 옆)
	 */
	public int calculateAisleDistance(int rowNo, int groupStartCol, int groupEndCol) {
		RowPattern pattern = findRowPattern(rowNo);

		int rowStartCol = pattern.startTemplateColNo();
		int rowEndCol = rowStartCol + pattern.seatCount() - 1;

		int leftDistance = groupStartCol - rowStartCol;
		int rightDistance = rowEndCol - groupEndCol;

		return Math.min(leftDistance, rightDistance);
	}

	private RowPattern findRowPattern(int rowNo) {
		for (RowPattern pattern : LAYOUT) {
			if (pattern.rowNo() == rowNo) {
				return pattern;
			}
		}
		throw new IllegalArgumentException("Unknown row number: " + rowNo);
	}
}
