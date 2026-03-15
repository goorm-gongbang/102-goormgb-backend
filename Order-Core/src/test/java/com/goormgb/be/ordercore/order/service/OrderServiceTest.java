package com.goormgb.be.ordercore.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.domain.ticket.enums.TicketType;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.order.dto.request.OrderCreateRequest;
import com.goormgb.be.ordercore.order.dto.request.SeatOrderItem;
import com.goormgb.be.ordercore.order.dto.response.OrderCreateResponse;
import com.goormgb.be.ordercore.order.dto.response.OrderSheetGetResponse;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.query.SeatHoldInfo;
import com.goormgb.be.ordercore.order.query.SeatInfoQueryService;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.order.repository.OrderSeatRepository;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 서비스 단위 테스트")
class OrderServiceTest {

	@Mock
	private MatchRepository matchRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private OrderRepository orderRepository;
	@Mock
	private OrderSeatRepository orderSeatRepository;
	@Mock
	private SeatInfoQueryService seatInfoQueryService;

	private OrderService orderService;

	@BeforeEach
	void setUp() {
		orderService = new OrderService(
			matchRepository, userRepository, orderRepository, orderSeatRepository, seatInfoQueryService
		);
	}

	@Nested
	@DisplayName("getOrderSheet — 주문서 조회")
	class GetOrderSheet {

		@Test
		@DisplayName("유효한 요청이면 주문서를 반환한다")
		void getOrderSheet_성공() {
			Long userId = 1L;
			Long matchId = 1L;
			List<Long> seatIds = List.of(101L);
			Match match = OrderFixture.createWeekdayMatch();
			SeatHoldInfo holdInfo = OrderFixture.createSeatHoldInfo(101L, userId);

			given(matchRepository.findDetailByIdOrThrow(matchId)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, seatIds)).willReturn(List.of(holdInfo));
			given(seatInfoQueryService.findPrice(1L, "WEEKDAY", "ADULT")).willReturn(22000);

			OrderSheetGetResponse response = orderService.getOrderSheet(userId, matchId, seatIds);

			assertThat(response).isNotNull();
			assertThat(response.seats()).hasSize(1);
			assertThat(response.seats().get(0).matchSeatId()).isEqualTo(101L);
			assertThat(response.seats().get(0).adultPrice()).isEqualTo(22000);
			assertThat(response.summary().seatCount()).isEqualTo(1);
			assertThat(response.summary().bookingFee()).isEqualTo(2000);
		}

		@Test
		@DisplayName("주말 경기이면 WEEKEND dayType으로 가격을 조회한다")
		void getOrderSheet_주말경기_WEEKEND_dayType() {
			Long userId = 1L;
			Long matchId = 2L;
			List<Long> seatIds = List.of(101L);
			Match weekendMatch = OrderFixture.createWeekendMatch();
			SeatHoldInfo holdInfo = OrderFixture.createSeatHoldInfo(101L, userId);

			given(matchRepository.findDetailByIdOrThrow(matchId)).willReturn(weekendMatch);
			given(seatInfoQueryService.findSeatHoldInfos(userId, seatIds)).willReturn(List.of(holdInfo));
			given(seatInfoQueryService.findPrice(1L, "WEEKEND", "ADULT")).willReturn(24000);

			OrderSheetGetResponse response = orderService.getOrderSheet(userId, matchId, seatIds);

			assertThat(response.seats().get(0).adultPrice()).isEqualTo(24000);
			then(seatInfoQueryService).should().findPrice(1L, "WEEKEND", "ADULT");
		}

		@Test
		@DisplayName("복수 좌석 주문서 조회 시 각 좌석 정보가 모두 포함된다")
		void getOrderSheet_복수좌석_성공() {
			Long userId = 1L;
			Long matchId = 1L;
			List<Long> seatIds = List.of(101L, 102L);
			Match match = OrderFixture.createWeekdayMatch();
			SeatHoldInfo hold1 = OrderFixture.createSeatHoldInfo(101L, userId);
			SeatHoldInfo hold2 = new SeatHoldInfo(
				2L, 102L, userId,
				Instant.now().plus(10, ChronoUnit.MINUTES),
				1L, "블루석", 10L, "BLUE_02", 6, 1
			);

			given(matchRepository.findDetailByIdOrThrow(matchId)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, seatIds)).willReturn(List.of(hold1, hold2));
			given(seatInfoQueryService.findPrice(eq(1L), eq("WEEKDAY"), eq("ADULT"))).willReturn(22000);

			OrderSheetGetResponse response = orderService.getOrderSheet(userId, matchId, seatIds);

			assertThat(response.seats()).hasSize(2);
			assertThat(response.summary().seatCount()).isEqualTo(2);
		}

		@Test
		@DisplayName("seatIds가 비어있으면 ORDER_SEAT_EMPTY 예외가 발생한다")
		void getOrderSheet_빈seatIds_예외() {
			assertThatThrownBy(
				() -> orderService.getOrderSheet(1L, 1L, Collections.emptyList())
			)
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ORDER_SEAT_EMPTY.getMessage());
		}

		@Test
		@DisplayName("선점 정보가 누락된 경우 SEAT_HOLD_NOT_FOUND 예외가 발생한다")
		void getOrderSheet_선점_미발견_예외() {
			Long userId = 1L;
			Match match = OrderFixture.createWeekdayMatch();
			given(matchRepository.findDetailByIdOrThrow(1L)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L, 102L)))
				.willReturn(List.of(OrderFixture.createSeatHoldInfo(101L, userId))); // 1개만 반환

			assertThatThrownBy(
				() -> orderService.getOrderSheet(userId, 1L, List.of(101L, 102L))
			)
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.SEAT_HOLD_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("선점이 만료된 경우 SEAT_HOLD_EXPIRED 예외가 발생한다")
		void getOrderSheet_선점_만료_예외() {
			Long userId = 1L;
			Match match = OrderFixture.createWeekdayMatch();
			SeatHoldInfo expiredHold = OrderFixture.createExpiredSeatHoldInfo(101L, userId);

			given(matchRepository.findDetailByIdOrThrow(1L)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L))).willReturn(List.of(expiredHold));

			assertThatThrownBy(
				() -> orderService.getOrderSheet(userId, 1L, List.of(101L))
			)
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.SEAT_HOLD_EXPIRED.getMessage());
		}

		@Test
		@DisplayName("가격 정책이 없는 경우 PRICE_POLICY_NOT_FOUND 예외가 발생한다")
		void getOrderSheet_가격정책_미발견_예외() {
			Long userId = 1L;
			Match match = OrderFixture.createWeekdayMatch();
			SeatHoldInfo holdInfo = OrderFixture.createSeatHoldInfo(101L, userId);

			given(matchRepository.findDetailByIdOrThrow(1L)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L))).willReturn(List.of(holdInfo));
			given(seatInfoQueryService.findPrice(anyLong(), anyString(), anyString())).willReturn(null);

			assertThatThrownBy(
				() -> orderService.getOrderSheet(userId, 1L, List.of(101L))
			)
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.PRICE_POLICY_NOT_FOUND.getMessage());
		}
	}

	@Nested
	@DisplayName("createOrder — 주문 생성")
	class CreateOrder {

		private void stubSaveOrder(Long orderId) {
			given(orderRepository.save(any(Order.class))).willAnswer(inv -> {
				Order o = inv.getArgument(0);
				ReflectionTestUtils.setField(o, "id", orderId);
				ReflectionTestUtils.setField(o, "createdAt", Instant.now());
				return o;
			});
		}

		@Test
		@DisplayName("유효한 단일 좌석 주문 시 totalAmount = 티켓가격 + 2000이다")
		void createOrder_단일좌석_totalAmount_계산() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			Match match = OrderFixture.createWeekdayMatch();
			SeatHoldInfo holdInfo = OrderFixture.createSeatHoldInfo(101L, userId);
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(matchRepository.findDetailByIdOrThrow(1L)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L))).willReturn(List.of(holdInfo));
			given(seatInfoQueryService.isAlreadyOrdered(101L)).willReturn(false);
			given(seatInfoQueryService.findPrice(1L, "WEEKDAY", "ADULT")).willReturn(22000);
			stubSaveOrder(1L);

			OrderCreateResponse response = orderService.createOrder(userId, request);

			assertThat(response.status()).isEqualTo(OrderStatus.PAYMENT_PENDING);
			assertThat(response.totalAmount()).isEqualTo(22000 + 2000);
			assertThat(response.bookingFee()).isEqualTo(2000);
			assertThat(response.seatCount()).isEqualTo(1);
			assertThat(response.matchId()).isEqualTo(1L);
		}

		@Test
		@DisplayName("복수 좌석 주문 시 totalAmount = 모든 티켓 합산 + 2000이다")
		void createOrder_복수좌석_totalAmount_계산() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			Match match = OrderFixture.createWeekdayMatch();
			SeatHoldInfo hold1 = OrderFixture.createSeatHoldInfo(101L, userId);
			SeatHoldInfo hold2 = new SeatHoldInfo(
				2L, 102L, userId,
				Instant.now().plus(10, ChronoUnit.MINUTES),
				1L, "블루석", 10L, "BLUE_02", 6, 1
			);
			OrderCreateRequest request = new OrderCreateRequest(
				1L,
				List.of(
					new SeatOrderItem(101L, TicketType.ADULT),
					new SeatOrderItem(102L, TicketType.YOUTH)
				),
				"홍길동", "hong@test.com", "010-1234-5678", "990831"
			);

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(matchRepository.findDetailByIdOrThrow(1L)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L, 102L)))
				.willReturn(List.of(hold1, hold2));
			given(seatInfoQueryService.isAlreadyOrdered(101L)).willReturn(false);
			given(seatInfoQueryService.isAlreadyOrdered(102L)).willReturn(false);
			given(seatInfoQueryService.findPrice(1L, "WEEKDAY", "ADULT")).willReturn(22000);
			given(seatInfoQueryService.findPrice(1L, "WEEKDAY", "YOUTH")).willReturn(7000);
			stubSaveOrder(1L);

			OrderCreateResponse response = orderService.createOrder(userId, request);

			assertThat(response.seatCount()).isEqualTo(2);
			assertThat(response.totalAmount()).isEqualTo(22000 + 7000 + 2000);
		}

		@Test
		@DisplayName("주말 경기 주문 시 WEEKEND 가격이 적용된다")
		void createOrder_주말경기_WEEKEND가격_적용() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			Match weekendMatch = OrderFixture.createWeekendMatch();
			SeatHoldInfo holdInfo = OrderFixture.createSeatHoldInfo(101L, userId);
			OrderCreateRequest request = new OrderCreateRequest(
				2L,
				List.of(new SeatOrderItem(101L, TicketType.ADULT)),
				"홍길동", "hong@test.com", "010-1234-5678", "990831"
			);

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(matchRepository.findDetailByIdOrThrow(2L)).willReturn(weekendMatch);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L))).willReturn(List.of(holdInfo));
			given(seatInfoQueryService.isAlreadyOrdered(101L)).willReturn(false);
			given(seatInfoQueryService.findPrice(1L, "WEEKEND", "ADULT")).willReturn(24000);
			stubSaveOrder(1L);

			orderService.createOrder(userId, request);

			then(seatInfoQueryService).should().findPrice(1L, "WEEKEND", "ADULT");
		}

		@Test
		@DisplayName("OrderSeat이 모두 저장된다")
		void createOrder_OrderSeat_저장() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			Match match = OrderFixture.createWeekdayMatch();
			SeatHoldInfo holdInfo = OrderFixture.createSeatHoldInfo(101L, userId);
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(matchRepository.findDetailByIdOrThrow(1L)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L))).willReturn(List.of(holdInfo));
			given(seatInfoQueryService.isAlreadyOrdered(101L)).willReturn(false);
			given(seatInfoQueryService.findPrice(1L, "WEEKDAY", "ADULT")).willReturn(22000);
			stubSaveOrder(1L);

			orderService.createOrder(userId, request);

			then(orderSeatRepository).should().saveAll(any());
		}

		@Test
		@DisplayName("seats가 비어있으면 ORDER_SEAT_EMPTY 예외가 발생한다")
		void createOrder_빈좌석_예외() {
			OrderCreateRequest request = new OrderCreateRequest(
				1L, Collections.emptyList(),
				"홍길동", "hong@test.com", "010-1234-5678", "990831"
			);

			assertThatThrownBy(() -> orderService.createOrder(1L, request))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ORDER_SEAT_EMPTY.getMessage());
		}

		@Test
		@DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
		void createOrder_사용자_미발견_예외() {
			Long userId = 999L;
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			assertThatThrownBy(() -> orderService.createOrder(userId, request))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("선점 정보가 누락된 경우 SEAT_HOLD_NOT_FOUND 예외가 발생한다")
		void createOrder_선점_미발견_예외() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			Match match = OrderFixture.createWeekdayMatch();
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(matchRepository.findDetailByIdOrThrow(1L)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L))).willReturn(Collections.emptyList());

			assertThatThrownBy(() -> orderService.createOrder(userId, request))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.SEAT_HOLD_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("선점이 만료된 경우 SEAT_HOLD_EXPIRED 예외가 발생한다")
		void createOrder_선점_만료_예외() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			Match match = OrderFixture.createWeekdayMatch();
			SeatHoldInfo expiredHold = OrderFixture.createExpiredSeatHoldInfo(101L, userId);
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(matchRepository.findDetailByIdOrThrow(1L)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L))).willReturn(List.of(expiredHold));

			assertThatThrownBy(() -> orderService.createOrder(userId, request))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.SEAT_HOLD_EXPIRED.getMessage());
		}

		@Test
		@DisplayName("이미 주문된 좌석이면 INVALID_ORDER_STATUS 예외가 발생한다")
		void createOrder_이미_주문된_좌석_예외() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			Match match = OrderFixture.createWeekdayMatch();
			SeatHoldInfo holdInfo = OrderFixture.createSeatHoldInfo(101L, userId);
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(matchRepository.findDetailByIdOrThrow(1L)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L))).willReturn(List.of(holdInfo));
			given(seatInfoQueryService.isAlreadyOrdered(101L)).willReturn(true);

			assertThatThrownBy(() -> orderService.createOrder(userId, request))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());
		}

		@Test
		@DisplayName("가격 정책이 없는 경우 PRICE_POLICY_NOT_FOUND 예외가 발생한다")
		void createOrder_가격정책_미발견_예외() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			Match match = OrderFixture.createWeekdayMatch();
			SeatHoldInfo holdInfo = OrderFixture.createSeatHoldInfo(101L, userId);
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(matchRepository.findDetailByIdOrThrow(1L)).willReturn(match);
			given(seatInfoQueryService.findSeatHoldInfos(userId, List.of(101L))).willReturn(List.of(holdInfo));
			given(seatInfoQueryService.isAlreadyOrdered(101L)).willReturn(false);
			given(seatInfoQueryService.findPrice(anyLong(), anyString(), anyString())).willReturn(null);

			assertThatThrownBy(() -> orderService.createOrder(userId, request))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.PRICE_POLICY_NOT_FOUND.getMessage());
		}
	}
}
