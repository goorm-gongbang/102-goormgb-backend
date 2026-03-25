package com.goormgb.be.ordercore.mypage.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.ordercore.mypage.dto.query.TicketDetailBaseRow;
import com.goormgb.be.ordercore.mypage.dto.query.TicketSeatDetailRow;

public record MyPageTicketQrResponse(
	Long ticketId,
	String qrToken,
	Instant expiresAt,
	MatchInfo match,
	List<SeatInfo> seats
) {

	public static MyPageTicketQrResponse of(
		TicketDetailBaseRow base,
		List<TicketSeatDetailRow> seatRows,
		String qrToken,
		Instant expiresAt
	) {
		return new MyPageTicketQrResponse(
			base.orderId(),
			qrToken,
			expiresAt,
			MatchInfo.from(base),
			seatRows.stream().map(SeatInfo::from).toList()
		);
	}

	public record MatchInfo(
		Instant matchAt,
		ClubInfo homeClub,
		ClubInfo awayClub,
		String stadiumName
	) {
		public static MatchInfo from(TicketDetailBaseRow base) {
			return new MatchInfo(
				base.matchAt(),
				new ClubInfo(base.homeClubName()),
				new ClubInfo(base.awayClubName()),
				base.stadiumName()
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
