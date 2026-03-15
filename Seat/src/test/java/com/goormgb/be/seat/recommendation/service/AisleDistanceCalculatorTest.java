package com.goormgb.be.seat.recommendation.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AisleDistanceCalculatorTest {

	private final AisleDistanceCalculator calculator = new AisleDistanceCalculator();

	@Test
	@DisplayName("14칸 row에서 왼쪽 끝 좌석은 통로 거리가 0이다")
	void 왼쪽_끝_통로거리_0() {
		// row 1: 14칸, startCol=1
		// 좌석 그룹: [1, 2] → leftDist=0, rightDist=12
		int distance = calculator.calculateAisleDistance(1, 1, 2);
		assertThat(distance).isEqualTo(0);
	}

	@Test
	@DisplayName("14칸 row에서 오른쪽 끝 좌석은 통로 거리가 0이다")
	void 오른쪽_끝_통로거리_0() {
		// row 1: 14칸, startCol=1, endCol=14
		// 좌석 그룹: [13, 14] → leftDist=12, rightDist=0
		int distance = calculator.calculateAisleDistance(1, 13, 14);
		assertThat(distance).isEqualTo(0);
	}

	@Test
	@DisplayName("14칸 row에서 중앙 좌석은 통로 거리가 5이다")
	void 중앙_통로거리() {
		// row 1: 14칸, startCol=1
		// 좌석 그룹: [6, 7, 8, 9] → leftDist=5, rightDist=5
		int distance = calculator.calculateAisleDistance(1, 6, 9);
		assertThat(distance).isEqualTo(5);
	}

	@Test
	@DisplayName("7칸 row(4~7행)에서 왼쪽 끝 좌석은 통로 거리가 0이다")
	void 짧은_row_왼쪽_끝() {
		// row 4: 7칸, startCol=5
		// 좌석 그룹: [5, 6] → leftDist=0, rightDist=5
		int distance = calculator.calculateAisleDistance(4, 5, 6);
		assertThat(distance).isEqualTo(0);
	}

	@Test
	@DisplayName("7칸 row(4~7행)에서 오른쪽 끝 좌석은 통로 거리가 0이다")
	void 짧은_row_오른쪽_끝() {
		// row 5: 7칸, startCol=5, endCol=11
		// 좌석 그룹: [10, 11] → leftDist=5, rightDist=0
		int distance = calculator.calculateAisleDistance(5, 10, 11);
		assertThat(distance).isEqualTo(0);
	}

	@Test
	@DisplayName("단일 좌석의 통로 거리를 정확히 계산한다")
	void 단일_좌석_통로거리() {
		// row 8: 14칸, startCol=1
		// 좌석 [3] → leftDist=2, rightDist=11
		int distance = calculator.calculateAisleDistance(8, 3, 3);
		assertThat(distance).isEqualTo(2);
	}
}
