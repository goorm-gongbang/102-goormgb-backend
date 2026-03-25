package com.goormgb.be.ordercore.mypage.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.ordercore.mypage.dto.query.TicketSeatDetailRow;
import com.goormgb.be.ordercore.order.entity.Order;

public record MyPageTicketQrResponse(
	Long ticketId,
	String qrToken,
	Instant expiresAt,
	MatchInfo match,
	List<SeatInfo> seats
) {

	public static MyPageTicketQrResponse of(
		Order order,
		List<TicketSeatDetailRow> seatRows,
		String qrToken,
		Instant expiresAt
	) {
		return new MyPageTicketQrResponse(
			order.getId(),
			qrToken,
			expiresAt,
			MatchInfo.from(order),
			seatRows.stream().map(SeatInfo::from).toList()
		);
	}

	public record MatchInfo(
		Instant matchAt,
		ClubInfo homeClub,
		ClubInfo awayClub,
		String stadiumName
	) {
		public static MatchInfo from(Order order) {
			return new MatchInfo(
				order.getMatch().getMatchAt(),
				new ClubInfo(order.getMatch().getHomeClub().getKoName()),
				new ClubInfo(order.getMatch().getAwayClub().getKoName()),
				order.getMatch().getStadium().getKoName()
			);
		}
	}

	public record ClubInfo(
		String koName
	) {
	}

	public record SeatInfo(
		String sectionName,
		String blockCode,
		int rowNo,
		int seatNo
	) {
		public static SeatInfo from(TicketSeatDetailRow row) {
			return new SeatInfo(
				row.sectionName(),
				row.blockCode(),
				row.rowNo(),
				row.seatNo()
			);
		}
	}
}
