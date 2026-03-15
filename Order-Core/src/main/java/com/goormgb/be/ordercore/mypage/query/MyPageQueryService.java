package com.goormgb.be.ordercore.mypage.query;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.goormgb.be.ordercore.order.enums.OrderStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyPageQueryService {

	private final NamedParameterJdbcTemplate namedJdbc;

	/**
	 * 탭 조건에 해당하는 티켓 수를 반환한다.
	 */
	public long countTickets(Long userId, List<String> statuses) {
		String sql = """
				SELECT COUNT(*)
				FROM orders
				WHERE user_id = :userId
				  AND status IN (:statuses)
				""";

		var params = new MapSqlParameterSource()
				.addValue("userId", userId)
				.addValue("statuses", statuses);

		Long count = namedJdbc.queryForObject(sql, params, Long.class);
		return count != null ? count : 0L;
	}

	/**
	 * 탭 조건에 해당하는 티켓 목록을 페이지네이션하여 반환한다.
	 */
	public List<TicketRow> findTickets(Long userId, List<String> statuses, int page, int size) {
		String sql = """
				SELECT
				    o.id           AS order_id,
				    o.status,
				    m.match_at,
				    hc.id          AS home_club_id,
				    hc.ko_name     AS home_club_name,
				    ac.id          AS away_club_id,
				    ac.ko_name     AS away_club_name,
				    st.ko_name     AS stadium_name,
				    (SELECT COUNT(*) FROM order_seats os2 WHERE os2.order_id = o.id) AS seat_count
				FROM orders o
				JOIN matches m  ON o.match_id    = m.id
				JOIN clubs hc   ON m.home_club_id = hc.id
				JOIN clubs ac   ON m.away_club_id = ac.id
				JOIN stadiums st ON m.stadium_id  = st.id
				WHERE o.user_id = :userId
				  AND o.status IN (:statuses)
				ORDER BY o.created_at DESC
				LIMIT :size OFFSET :offset
				""";

		var params = new MapSqlParameterSource()
				.addValue("userId", userId)
				.addValue("statuses", statuses)
				.addValue("size", size)
				.addValue("offset", (long)page * size);

		return namedJdbc.query(sql, params, (rs, rowNum) -> new TicketRow(
				rs.getLong("order_id"),
				OrderStatus.valueOf(rs.getString("status")),
				rs.getObject("match_at", Timestamp.class).toInstant(),
				rs.getLong("home_club_id"),
				rs.getString("home_club_name"),
				rs.getLong("away_club_id"),
				rs.getString("away_club_name"),
				rs.getString("stadium_name"),
				rs.getInt("seat_count")
		));
	}

	/**
	 * 주문 ID 목록에 해당하는 좌석 정보를 orderId와 함께 반환한다.
	 */
	public List<OrderSeatRow> findOrderSeatRowsByOrderIds(List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return Collections.emptyList();
		}

		String sql = """
				SELECT
				    os.order_id,
				    sec.name    AS section_name,
				    b.block_code,
				    os.row_no,
				    os.seat_no
				FROM order_seats os
				JOIN sections sec ON os.section_id = sec.id
				JOIN blocks b     ON os.block_id   = b.id
				WHERE os.order_id IN (:orderIds)
				ORDER BY os.order_id, os.id
				""";

		var params = new MapSqlParameterSource()
				.addValue("orderIds", orderIds);

		return namedJdbc.query(sql, params, (rs, rowNum) -> new OrderSeatRow(
				rs.getLong("order_id"),
				rs.getString("section_name"),
				rs.getString("block_code"),
				rs.getInt("row_no"),
				rs.getInt("seat_no")
		));
	}

	public record TicketRow(
		Long orderId,
		OrderStatus status,
		java.time.Instant matchAt,
		Long homeClubId,
		String homeClubName,
		Long awayClubId,
		String awayClubName,
		String stadiumName,
		int seatCount
	) {
	}

	public record OrderSeatRow(
		Long orderId,
		String sectionName,
		String blockCode,
		int rowNo,
		int seatNo
	) {
	}
}
