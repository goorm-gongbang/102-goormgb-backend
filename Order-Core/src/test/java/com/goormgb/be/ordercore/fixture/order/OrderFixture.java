package com.goormgb.be.ordercore.fixture.order;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.stadium.entity.Stadium;
import com.goormgb.be.domain.ticket.enums.TicketType;
import com.goormgb.be.ordercore.order.dto.request.OrderCreateRequest;
import com.goormgb.be.ordercore.order.dto.request.SeatOrderItem;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.entity.OrderSeat;
import com.goormgb.be.ordercore.order.query.SeatHoldInfo;
import com.goormgb.be.user.entity.User;

public final class OrderFixture {

	private OrderFixture() {
	}

	public static User createUser() {
		User user = User.builder()
			.email("test@test.com")
			.nickname("테스터")
			.profileImageUrl(null)
			.build();
		ReflectionTestUtils.setField(user, "id", 1L);
		return user;
	}

	public static User createUserWithId(Long id) {
		User user = User.builder()
			.email("user" + id + "@test.com")
			.nickname("테스터" + id)
			.profileImageUrl(null)
			.build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	public static Stadium createStadium() {
		Stadium stadium = Stadium.builder()
			.region("서울")
			.koName("잠실야구장")
			.enName("Jamsil Baseball Stadium")
			.address("서울특별시 송파구 올림픽로 19-2")
			.build();
		ReflectionTestUtils.setField(stadium, "id", 1L);
		return stadium;
	}

	public static Club createHomeClub(Stadium stadium) {
		Club club = Club.builder()
			.koName("LG 트윈스")
			.enName("LG Twins")
			.logoImg(null)
			.clubColor("#C60C30")
			.stadium(stadium)
			.homepageRedirectUrl(null)
			.build();
		ReflectionTestUtils.setField(club, "id", 1L);
		return club;
	}

	public static Club createAwayClub(Stadium stadium) {
		Club club = Club.builder()
			.koName("두산 베어스")
			.enName("Doosan Bears")
			.logoImg(null)
			.clubColor("#131230")
			.stadium(stadium)
			.homepageRedirectUrl(null)
			.build();
		ReflectionTestUtils.setField(club, "id", 2L);
		return club;
	}

	/** 평일 경기: 2026-03-11(수) 18:30 KST = 09:30 UTC */
	public static Match createWeekdayMatch() {
		Stadium stadium = createStadium();
		Instant matchAt = Instant.parse("2026-03-11T09:30:00Z");
		Match match = Match.builder()
			.matchAt(matchAt)
			.homeClub(createHomeClub(stadium))
			.awayClub(createAwayClub(stadium))
			.stadium(stadium)
			.saleStatus(SaleStatus.ON_SALE)
			.build();
		ReflectionTestUtils.setField(match, "id", 1L);
		return match;
	}

	/** 주말 경기: 2026-03-14(토) 14:00 KST = 05:00 UTC */
	public static Match createWeekendMatch() {
		Stadium stadium = createStadium();
		Instant matchAt = Instant.parse("2026-03-14T05:00:00Z");
		Match match = Match.builder()
			.matchAt(matchAt)
			.homeClub(createHomeClub(stadium))
			.awayClub(createAwayClub(stadium))
			.stadium(stadium)
			.saleStatus(SaleStatus.ON_SALE)
			.build();
		ReflectionTestUtils.setField(match, "id", 2L);
		return match;
	}

	public static Order createOrder(User user, Match match) {
		Order order = Order.builder()
			.user(user)
			.match(match)
			.totalAmount(24000)
			.ordererName("홍길동")
			.ordererEmail("hong@test.com")
			.ordererPhone("010-1234-5678")
			.ordererBirthDate("990831")
			.build();
		ReflectionTestUtils.setField(order, "id", 1L);
		ReflectionTestUtils.setField(order, "createdAt", Instant.now());
		return order;
	}

	public static Order createOrderWithId(Long id, User user, Match match, int totalAmount) {
		Order order = Order.builder()
			.user(user)
			.match(match)
			.totalAmount(totalAmount)
			.ordererName("홍길동")
			.ordererEmail("hong@test.com")
			.ordererPhone("010-1234-5678")
			.ordererBirthDate("990831")
			.build();
		ReflectionTestUtils.setField(order, "id", id);
		ReflectionTestUtils.setField(order, "createdAt", Instant.now());
		return order;
	}

	public static OrderSeat createOrderSeat(Order order) {
		OrderSeat seat = OrderSeat.builder()
			.order(order)
			.matchSeatId(101L)
			.blockId(10L)
			.sectionId(1L)
			.rowNo(5)
			.seatNo(12)
			.price(22000)
			.ticketType(TicketType.ADULT)
			.build();
		ReflectionTestUtils.setField(seat, "id", 1L);
		return seat;
	}

	/** 유효한 선점 (만료 10분 후) */
	public static SeatHoldInfo createSeatHoldInfo(Long matchSeatId, Long userId) {
		return new SeatHoldInfo(
			1L,
			matchSeatId,
			userId,
			Instant.now().plus(10, ChronoUnit.MINUTES),
			1L,
			"블루석",
			10L,
			"BLUE_01",
			5,
			12
		);
	}

	/** 만료된 선점 (1분 전 만료) */
	public static SeatHoldInfo createExpiredSeatHoldInfo(Long matchSeatId, Long userId) {
		return new SeatHoldInfo(
			2L,
			matchSeatId,
			userId,
			Instant.now().minus(1, ChronoUnit.MINUTES),
			1L,
			"블루석",
			10L,
			"BLUE_01",
			5,
			12
		);
	}

	public static SeatHoldInfo createSeatHoldInfoWithSection(Long holdId, Long matchSeatId, Long userId,
		Long sectionId, String sectionName) {
		return new SeatHoldInfo(
			holdId,
			matchSeatId,
			userId,
			Instant.now().plus(10, ChronoUnit.MINUTES),
			sectionId,
			sectionName,
			10L,
			"BLUE_0" + holdId,
			5,
			(int)(holdId * 10)
		);
	}

	public static OrderCreateRequest createOrderCreateRequest() {
		return new OrderCreateRequest(
			1L,
			List.of(new SeatOrderItem(101L, TicketType.ADULT)),
			"홍길동",
			"hong@test.com",
			"010-1234-5678",
			"990831"
		);
	}

	public static OrderCreateRequest createOrderCreateRequestWithSeats(Long matchId, List<SeatOrderItem> seats) {
		return new OrderCreateRequest(
			matchId,
			seats,
			"홍길동",
			"hong@test.com",
			"010-1234-5678",
			"990831"
		);
	}
}
