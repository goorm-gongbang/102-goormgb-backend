package com.goormgb.be.ordercore.mypage.service.support;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.OrderSeatRow;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.TicketRow;

public final class MyPageTicketListAssembler {

	private MyPageTicketListAssembler() {
	}

	public static Map<Long, List<MyPageTicketListResponse.SeatInfo>> toSeatMap(List<OrderSeatRow> seatRows) {
		return seatRows.stream()
			.collect(Collectors.groupingBy(
				OrderSeatRow::orderId,
				Collectors.mapping(
					row -> new MyPageTicketListResponse.SeatInfo(
						row.sectionName(), row.blockCode(), row.rowNo(), row.seatNo()
					),
					Collectors.toList()
				)
			));
	}

	public static List<MyPageTicketListResponse.TicketItem> toTicketItems(
		List<TicketRow> ticketRows,
		Map<Long, List<MyPageTicketListResponse.SeatInfo>> seatMap
	) {
		return ticketRows.stream()
			.map(row -> new MyPageTicketListResponse.TicketItem(
				row.orderId(),
				row.matchAt(),
				new MyPageTicketListResponse.ClubInfo(row.homeClubId(), row.homeClubName()),
				new MyPageTicketListResponse.ClubInfo(row.awayClubId(), row.awayClubName()),
				row.stadiumName(),
				row.seatCount(),
				seatMap.getOrDefault(row.orderId(), List.of()),
				row.status(),
				MyPageTicketListResponse.TicketActions.of(row.status(), row.matchAt())
			))
			.toList();
	}
}
