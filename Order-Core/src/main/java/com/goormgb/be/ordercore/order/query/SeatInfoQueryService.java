package com.goormgb.be.ordercore.order.query;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatInfoQueryService {

	private final NamedParameterJdbcTemplate namedJdbc;

	/**
	 * matchSeatIds에 해당하는 좌석 선점 정보와 좌석 상세 정보를 조회한다.
	 * user_id 검증과 만료 여부도 함께 수행한다.
	 */
	public List<SeatHoldInfo> findSeatHoldInfos(Long userId, List<Long> matchSeatIds) {
		String sql = """
				SELECT
					sh.id        AS hold_id,
					sh.match_seat_id,
					sh.user_id,
					sh.expires_at,
					ms.section_id, 
					sec.name     AS section_name,
					ms.block_id,
					b.block_code,
					ms.row_no,
					ms.seat_no
				FROM seat_holds sh
				JOIN match_seats ms  ON sh.match_seat_id = ms.id
				JOIN blocks b        ON ms.block_id       = b.id
				JOIN sections sec    ON ms.section_id     = sec.id
				WHERE sh.match_seat_id IN (:matchSeatIds)
				  AND sh.user_id = :userId
				""";

		var params = new MapSqlParameterSource()
				.addValue("matchSeatIds", matchSeatIds)
				.addValue("userId", userId);

		return namedJdbc.query(sql, params, (rs, rowNum) -> new SeatHoldInfo(
				rs.getLong("hold_id"),
				rs.getLong("match_seat_id"),
				rs.getLong("user_id"),
				rs.getObject("expires_at", Timestamp.class).toInstant(),
				rs.getLong("section_id"),
				rs.getString("section_name"),
				rs.getLong("block_id"),
				rs.getString("block_code"),
				rs.getInt("row_no"),
				rs.getInt("seat_no")
		));
	}

	/**
	 * (sectionId, dayType, ticketType) 조합에 해당하는 가격을 조회한다.
	 */
	public Integer findPrice(Long sectionId, String dayType, String ticketType) {
		String sql = """
				SELECT price
				FROM price_policies
				WHERE section_id  = :sectionId
				  AND day_type    = :dayType
				  AND ticket_type = :ticketType
				""";

		var params = Map.of(
				"sectionId", sectionId,
				"dayType", dayType,
				"ticketType", ticketType
		);

		List<Integer> results = namedJdbc.queryForList(sql, params, Integer.class);
		return results.isEmpty() ? null : results.get(0);
	}

	/**
	 * BLOCKED 상태인 좌석들을 SOLD로 일괄 전환한다.
	 * 결제 확정 시 호출되며, BLOCKED가 아닌 좌석은 건드리지 않는다.
	 *
	 * @return 변경된 행 수
	 */
	public int markSoldIfBlocked(List<Long> matchSeatIds) {
		if (matchSeatIds.isEmpty()) {
			return 0;
		}

		String sql = """
				UPDATE match_seats
				SET sale_status = 'SOLD'
				WHERE id IN (:matchSeatIds)
				  AND sale_status = 'BLOCKED'
				""";

		var params = new MapSqlParameterSource()
				.addValue("matchSeatIds", matchSeatIds);

		return namedJdbc.update(sql, params);
	}

	/**
	 * SOLD 상태인 좌석들을 AVAILABLE로 일괄 복원한다.
	 * 주문 취소 시 호출되며, SOLD가 아닌 좌석은 건드리지 않는다.
	 *
	 * @return 변경된 행 수
	 */
	public int markAvailableIfSold(List<Long> matchSeatIds) {
		if (matchSeatIds.isEmpty()) {
			return 0;
		}

		String sql = """
				UPDATE match_seats
				SET sale_status = 'AVAILABLE'
				WHERE id IN (:matchSeatIds)
				  AND sale_status = 'SOLD'
				""";

		var params = new MapSqlParameterSource()
				.addValue("matchSeatIds", matchSeatIds);

		return namedJdbc.update(sql, params);
	}

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
