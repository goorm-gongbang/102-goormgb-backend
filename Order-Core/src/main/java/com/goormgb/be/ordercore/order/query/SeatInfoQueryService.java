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
	 * matchSeatId로 이미 주문된 좌석인지 확인한다.
	 */
	public boolean isAlreadyOrdered(Long matchSeatId) {
		String sql = """
				SELECT COUNT(*)
				FROM order_seats
				WHERE match_seat_id = :matchSeatId
				""";

		var params = Map.of("matchSeatId", matchSeatId);
		Integer count = namedJdbc.queryForObject(sql, params, Integer.class);
		return count != null && count > 0;
	}
}
