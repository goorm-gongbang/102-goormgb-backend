package com.goormgb.be.ordercore.mypage.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.domain.ticket.enums.TicketType;
import com.goormgb.be.ordercore.mypage.dto.query.TicketDetailBaseRow;
import com.goormgb.be.ordercore.mypage.dto.query.TicketSeatDetailRow;
import com.goormgb.be.ordercore.order.enums.OrderStatus;

public record MyPageTicketDetailResponse(
	Long ticketId,
	OrderStatus status,
	MatchInfo match,
	List<SeatInfo> seats,
	PaymentInfo payment,
	CancellationPolicy cancellationPolicy,
	VirtualAccount virtualAccount,
	CancellationInfo cancellation,
	TicketActions actions
) {

	public static MyPageTicketDetailResponse of(
		TicketDetailBaseRow base,
		List<TicketSeatDetailRow> seatRows,
		PaymentInfo payment,
		CancellationPolicy cancellationPolicy,
		VirtualAccount virtualAccount,
		CancellationInfo cancellation
	) {
		return new MyPageTicketDetailResponse(
			base.orderId(),
			base.status(),
			MatchInfo.from(base),
			seatRows.stream()
				.map(SeatInfo::from)
				.toList(),
			payment,
			cancellationPolicy,
			virtualAccount,
			cancellation,
			TicketActions.of(base.status())
		);
	}

	public record MatchInfo(
		Long matchId,
		Instant matchAt,
		ClubInfo homeClub,
		ClubInfo awayClub,
		StadiumInfo stadium
	) {
		public static MatchInfo from(TicketDetailBaseRow base) {
			return new MatchInfo(
				base.matchId(),
				base.matchAt(),
				ClubInfo.home(base),
				ClubInfo.away(base),
				StadiumInfo.from(base)
			);
		}
	}

	public record ClubInfo(
		Long clubId,
		String koName
	) {
		public static ClubInfo home(TicketDetailBaseRow base) {
			return new ClubInfo(base.homeClubId(), base.homeClubName());
		}

		public static ClubInfo away(TicketDetailBaseRow base) {
			return new ClubInfo(base.awayClubId(), base.awayClubName());
		}
	}

	public record StadiumInfo(
		Long stadiumId,
		String koName,
		String address
	) {
		public static StadiumInfo from(TicketDetailBaseRow base) {
			return new StadiumInfo(
				base.stadiumId(),
				base.stadiumName(),
				base.stadiumAddress()
			);
		}
	}

	public record SeatInfo(
		String sectionName,
		String blockCode,
		int rowNo,
		int seatNo,
		int price,
		TicketType ticketType
	) {
		public static SeatInfo from(TicketSeatDetailRow row) {
			return new SeatInfo(
				row.sectionName(),
				row.blockCode(),
				row.rowNo(),
				row.seatNo(),
				row.price(),
				row.ticketType()
			);
		}
	}

	public record PaymentInfo(
		int totalAmount,
		int serviceFee,
		String paymentMethod,
		Instant paidAt,
		CashReceiptInfo cashReceipt
	) {
	}

	public record CashReceiptInfo(
		String type,
		String number,
		int totalAmount
	) {
	}

	public record CancellationPolicy(
		Instant deadline,
		String feeRate
	) {
	}

	public record VirtualAccount(
		String bank,
		String accountNumber,
		String holder,
		Instant depositDeadline
	) {
	}

	public record CancellationInfo(
		Instant cancelledAt,
		int cancellationFee,
		Integer refundedAmount
	) {
	}

	public record TicketActions(
		boolean canPrint
	) {
		public static TicketActions of(OrderStatus status) {
			return new TicketActions(status == OrderStatus.PAID);
		}
	}
}