package com.goormgb.be.seat.recommendation.dto.internal;

import java.util.List;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;

/**
 * 진짜 연석 후보를 나타내는 내부 DTO.
 * 같은 열(row)에서 templateColNo가 연속인 좌석 묶음이다.
 *
 * @param seats         연속 좌석 리스트 (templateColNo 오름차순)
 * @param rowNo         열 번호
 * @param startCol      시작 templateColNo
 * @param endCol        끝 templateColNo
 * @param aisleDistance  통로까지의 최소 거리
 */
public record SeatGroup(
	List<MatchSeat> seats,
	int rowNo,
	int startCol,
	int endCol,
	int aisleDistance
) {
}
