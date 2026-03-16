package com.goormgb.be.seat.recommendation.dto.internal;

import java.util.List;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;

/**
 * 준연석 후보를 나타내는 내부 DTO.
 * 인접한 2개 열에서 수평 겹침이 있는 좌석 묶음이다.
 *
 * @param upperSeats      위쪽 열의 좌석 리스트
 * @param lowerSeats      아래쪽 열의 좌석 리스트
 * @param upperRowNo      위쪽 열 번호
 * @param lowerRowNo      아래쪽 열 번호
 * @param overlapCount    수평 겹침 열 수
 * @param avgAisleDistance 평균 통로 거리
 */
public record SemiGroup(
	List<MatchSeat> upperSeats,
	List<MatchSeat> lowerSeats,
	int upperRowNo,
	int lowerRowNo,
	int overlapCount,
	int avgAisleDistance
) {

	public List<MatchSeat> allSeats() {
		List<MatchSeat> all = new java.util.ArrayList<>(upperSeats);
		all.addAll(lowerSeats);
		return all;
	}

	public int rowSum() {
		return upperRowNo + lowerRowNo;
	}
}
