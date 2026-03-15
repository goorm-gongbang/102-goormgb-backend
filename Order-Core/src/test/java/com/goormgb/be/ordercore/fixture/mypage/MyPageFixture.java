package com.goormgb.be.ordercore.fixture.mypage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.OrderSeatRow;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.TicketRow;
import com.goormgb.be.ordercore.order.enums.OrderStatus;

public final class MyPageFixture {

	private MyPageFixture() {
	}

	public static MyPageProfileResponse createProfileResponse() {
		return new MyPageProfileResponse(
			new MyPageProfileResponse.ProfileInfo("goorm123", null, "KAKAO"),
			new MyPageProfileResponse.TicketSummary(2, 1, 5)
		);
	}

	public static TicketRow createTicketRow(Long orderId, OrderStatus status) {
		return new TicketRow(
			orderId,
			status,
			Instant.now().plus(30, ChronoUnit.DAYS),
			1L, "LG 트윈스",
			2L, "두산 베어스",
			"잠실야구장",
			2
		);
	}

	public static TicketRow createPastTicketRow(Long orderId, OrderStatus status) {
		return new TicketRow(
			orderId,
			status,
			Instant.now().minus(10, ChronoUnit.DAYS),
			1L, "LG 트윈스",
			2L, "두산 베어스",
			"잠실야구장",
			1
		);
	}

	public static OrderSeatRow createOrderSeatRow(Long orderId) {
		return new OrderSeatRow(orderId, "오렌지석", "201", 8, 13);
	}

	public static List<MyPageTicketListResponse.SeatInfo> createSeatInfoList() {
		return List.of(
			new MyPageTicketListResponse.SeatInfo("오렌지석", "201", 8, 13),
			new MyPageTicketListResponse.SeatInfo("오렌지석", "201", 8, 14)
		);
	}

	public static MyPageTicketListResponse createTicketListResponse() {
		Instant futureMatchAt = Instant.now().plus(30, ChronoUnit.DAYS);
		MyPageTicketListResponse.TicketItem ticket = new MyPageTicketListResponse.TicketItem(
			101L,
			futureMatchAt,
			new MyPageTicketListResponse.ClubInfo(1L, "LG 트윈스"),
			new MyPageTicketListResponse.ClubInfo(2L, "두산 베어스"),
			"잠실야구장",
			2,
			createSeatInfoList(),
			OrderStatus.PAID,
			MyPageTicketListResponse.TicketActions.of(OrderStatus.PAID, futureMatchAt)
		);

		return MyPageTicketListResponse.of(
			8, 2, 1, 5,
			"BOOKED", 0, 10, 1L, 1, false,
			List.of(ticket)
		);
	}
}
