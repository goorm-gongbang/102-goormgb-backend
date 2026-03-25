package com.goormgb.be.ordercore.fixture.mypage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.goormgb.be.domain.ticket.enums.TicketType;
import com.goormgb.be.ordercore.mypage.dto.query.TicketDetailBaseRow;
import com.goormgb.be.ordercore.mypage.dto.query.TicketSeatDetailRow;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketCancelResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketDetailResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketQrResponse;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.OrderSeatRow;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.TicketRow;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.payment.enums.CashReceiptPurpose;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;

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

	public static TicketDetailBaseRow createTicketDetailBaseRow(Long orderId, OrderStatus status) {
		return createTicketDetailBaseRow(1L, orderId, status);
	}

	public static TicketDetailBaseRow createTicketDetailBaseRow(Long userId, Long orderId, OrderStatus status) {
		Instant futureMatchAt = Instant.now().plus(15, ChronoUnit.DAYS);
		return new TicketDetailBaseRow(
			orderId,
			userId,
			status,
			42000,
			2000,
			status == OrderStatus.CANCELLED ? Instant.now().minus(1, ChronoUnit.DAYS) : null,
			status == OrderStatus.CANCELLED ? 4200 : 0,
			status == OrderStatus.CANCELLED ? 37800 : null,
			55L,
			futureMatchAt,
			1L,
			"LG 트윈스",
			2L,
			"두산 베어스",
			3L,
			"잠실야구장",
			"서울특별시 송파구 올림픽로 19-2",
			status == OrderStatus.PAYMENT_PENDING ? PaymentMethod.BANK_TRANSFER : PaymentMethod.TOSS_PAY,
			status == OrderStatus.PAID ? Instant.now().minus(2, ChronoUnit.DAYS) : null,
			status == OrderStatus.PAYMENT_PENDING ? "신한은행" : null,
			status == OrderStatus.PAYMENT_PENDING ? "110-123-456789" : null,
			status == OrderStatus.PAYMENT_PENDING ? "주식회사 구름공방" : null,
			status == OrderStatus.PAYMENT_PENDING ? Instant.now().plus(1, ChronoUnit.DAYS) : null,
			status == OrderStatus.PAID ? CashReceiptPurpose.PERSONAL_DEDUCTION : null,
			status == OrderStatus.PAID ? "010-1234-5678" : null
		);
	}

	public static List<TicketSeatDetailRow> createTicketSeatDetailRows() {
		return List.of(
			new TicketSeatDetailRow("오렌지석", "206", 3, 13, 20000, TicketType.ADULT),
			new TicketSeatDetailRow("오렌지석", "206", 3, 14, 20000, TicketType.ADULT)
		);
	}

	public static MyPageTicketDetailResponse createTicketDetailResponse() {
		Instant futureMatchAt = Instant.now().plus(15, ChronoUnit.DAYS);
		return new MyPageTicketDetailResponse(
			101L,
			OrderStatus.PAID,
			new MyPageTicketDetailResponse.MatchInfo(
				55L,
				futureMatchAt,
				new MyPageTicketDetailResponse.ClubInfo(1L, "LG 트윈스"),
				new MyPageTicketDetailResponse.ClubInfo(2L, "KT 위즈"),
				new MyPageTicketDetailResponse.StadiumInfo(3L, "잠실야구장", "서울특별시 송파구 올림픽로 19-2")
			),
			List.of(
				new MyPageTicketDetailResponse.SeatInfo("오렌지석", "206", 3, 13, 20000, TicketType.ADULT),
				new MyPageTicketDetailResponse.SeatInfo("오렌지석", "206", 3, 14, 20000, TicketType.ADULT)
			),
			new MyPageTicketDetailResponse.PaymentInfo(
				42000,
				2000,
				"TOSS_PAY",
				Instant.now().minus(2, ChronoUnit.DAYS),
				new MyPageTicketDetailResponse.CashReceiptInfo("PERSONAL_DEDUCTION", "010-1234-5678", 42000)
			),
			new MyPageTicketDetailResponse.CancellationPolicy(
				futureMatchAt.minus(1, ChronoUnit.DAYS),
				"10%"
			),
			null,
			null,
			new MyPageTicketDetailResponse.TicketActions(true)
		);
	}

	public static MyPageTicketQrResponse createTicketQrResponse() {
		Instant matchAt = Instant.now().plus(1, ChronoUnit.HOURS);
		return new MyPageTicketQrResponse(
			101L,
			"qr-token-uuid",
			Instant.now().plus(2, ChronoUnit.MINUTES),
			new MyPageTicketQrResponse.MatchInfo(
				matchAt,
				new MyPageTicketQrResponse.ClubInfo("LG 트윈스"),
				new MyPageTicketQrResponse.ClubInfo("KT 위즈"),
				"잠실야구장"
			),
			List.of(
				new MyPageTicketQrResponse.SeatInfo("오렌지석", "206", 3, 13),
				new MyPageTicketQrResponse.SeatInfo("오렌지석", "206", 3, 14)
			)
		);
	}

	public static MyPageTicketCancelResponse createTicketCancelResponse() {
		return new MyPageTicketCancelResponse(
			101L,
			OrderStatus.CANCEL_REQUESTED,
			42000,
			6000,
			36000
		);
	}
}
