package com.goormgb.be.ordercore.order.query;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Order DB 자체 테이블만 조회하는 쿼리 서비스.
 * Seat DB 직접 조회 메서드(findSeatHoldInfos, findPrice, markSoldIfBlocked, markAvailableIfSold)는
 * SeatInternalClient로 이관 완료되어 제거되었다.
 */
@Service
@RequiredArgsConstructor
public class SeatInfoQueryService {

	private final NamedParameterJdbcTemplate namedJdbc;

	/**
	 * matchSeatId로 이미 유효한 주문이 존재하는 좌석인지 확인한다.
	 * 결제 완료(PAID), 취소 요청(CANCEL_REQUESTED), 환불 처리 중(REFUND_PROCESSING) 상태만 유효 주문으로 간주한다.
	 */
	public boolean isAlreadyOrdered(Long matchSeatId) {
		String sql = """
				SELECT COUNT(*)
				FROM order_seats os
				JOIN orders o ON os.order_id = o.id
				WHERE os.match_seat_id = :matchSeatId
				  AND o.status IN ('PAID', 'CANCEL_REQUESTED', 'REFUND_PROCESSING')
				""";

		var params = Map.of("matchSeatId", matchSeatId);
		Integer count = namedJdbc.queryForObject(sql, params, Integer.class);
		return count != null && count > 0;
	}
}
