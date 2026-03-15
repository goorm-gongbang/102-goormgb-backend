package com.goormgb.be.ordercore.order.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.goormgb.be.domain.ticket.enums.TicketType;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.order.dto.request.OrderCreateRequest;
import com.goormgb.be.ordercore.order.dto.request.SeatOrderItem;
import com.goormgb.be.ordercore.order.dto.response.OrderCreateResponse;
import com.goormgb.be.ordercore.order.dto.response.OrderSheetGetResponse;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.service.OrderService;
import com.goormgb.be.ordercore.support.WebMvcTestSupport;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController 슬라이스 테스트")
class OrderControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private OrderService orderService;

	private void setAuthentication(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")))
		);
	}

	private OrderSheetGetResponse createMockOrderSheetResponse() {
		OrderSheetGetResponse.ClubInfo homeClub = new OrderSheetGetResponse.ClubInfo(1L, "LG 트윈스");
		OrderSheetGetResponse.ClubInfo awayClub = new OrderSheetGetResponse.ClubInfo(2L, "두산 베어스");
		OrderSheetGetResponse.StadiumInfo stadium = new OrderSheetGetResponse.StadiumInfo(
			1L, "잠실야구장", "서울특별시 송파구 올림픽로 19-2"
		);
		OrderSheetGetResponse.MatchInfo matchInfo = new OrderSheetGetResponse.MatchInfo(
			1L, Instant.parse("2026-03-11T09:30:00Z"), homeClub, awayClub, stadium
		);
		OrderSheetGetResponse.SeatInfo seatInfo = new OrderSheetGetResponse.SeatInfo(
			101L, 1L, "블루석", 10L, "BLUE_01", 5, 12, 22000
		);
		return new OrderSheetGetResponse(
			matchInfo,
			List.of(seatInfo),
			new OrderSheetGetResponse.Summary(1, 2000)
		);
	}

	private OrderCreateResponse createMockOrderCreateResponse() {
		return new OrderCreateResponse(1L, OrderStatus.PAYMENT_PENDING, 1L, 1, 24000, 2000, Instant.now());
	}

	@Nested
	@DisplayName("GET /mypage/orders/sheet — 주문서 조회")
	class GetOrderSheet {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 요청이면 200과 주문서 정보를 반환한다")
		void getOrderSheet_성공() throws Exception {
			given(orderService.getOrderSheet(eq(1L), eq(1L), eq(List.of(101L))))
				.willReturn(createMockOrderSheetResponse());

			mockMvc.perform(get("/mypage/orders/sheet")
					.param("matchId", "1")
					.param("seatIds", "101"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.data.match.matchId").value(1))
				.andExpect(jsonPath("$.data.match.homeClub.koName").value("LG 트윈스"))
				.andExpect(jsonPath("$.data.match.awayClub.koName").value("두산 베어스"))
				.andExpect(jsonPath("$.data.match.stadium.koName").value("잠실야구장"))
				.andExpect(jsonPath("$.data.seats").isArray())
				.andExpect(jsonPath("$.data.seats.length()").value(1))
				.andExpect(jsonPath("$.data.seats[0].matchSeatId").value(101))
				.andExpect(jsonPath("$.data.seats[0].adultPrice").value(22000))
				.andExpect(jsonPath("$.data.summary.seatCount").value(1))
				.andExpect(jsonPath("$.data.summary.bookingFee").value(2000));
		}

		@Test
		@DisplayName("복수 좌석 ID로 주문서를 조회할 수 있다")
		void getOrderSheet_복수좌석_성공() throws Exception {
			OrderSheetGetResponse.ClubInfo homeClub = new OrderSheetGetResponse.ClubInfo(1L, "LG 트윈스");
			OrderSheetGetResponse.ClubInfo awayClub = new OrderSheetGetResponse.ClubInfo(2L, "두산 베어스");
			OrderSheetGetResponse.StadiumInfo stadium = new OrderSheetGetResponse.StadiumInfo(1L, "잠실야구장", "주소");
			OrderSheetGetResponse.MatchInfo matchInfo = new OrderSheetGetResponse.MatchInfo(
				1L, Instant.now(), homeClub, awayClub, stadium
			);
			List<OrderSheetGetResponse.SeatInfo> seats = List.of(
				new OrderSheetGetResponse.SeatInfo(101L, 1L, "블루석", 10L, "BLUE_01", 5, 12, 22000),
				new OrderSheetGetResponse.SeatInfo(102L, 1L, "블루석", 10L, "BLUE_02", 5, 13, 22000)
			);
			OrderSheetGetResponse response = new OrderSheetGetResponse(
				matchInfo, seats, new OrderSheetGetResponse.Summary(2, 2000)
			);

			given(orderService.getOrderSheet(eq(1L), eq(1L), eq(List.of(101L, 102L))))
				.willReturn(response);

			mockMvc.perform(get("/mypage/orders/sheet")
					.param("matchId", "1")
					.param("seatIds", "101", "102"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.seats.length()").value(2))
				.andExpect(jsonPath("$.data.summary.seatCount").value(2));
		}

		@Test
		@DisplayName("선점 만료 시 400을 반환한다")
		void getOrderSheet_선점_만료_400() throws Exception {
			given(orderService.getOrderSheet(any(), any(), any()))
				.willThrow(new CustomException(ErrorCode.SEAT_HOLD_EXPIRED));

			mockMvc.perform(get("/mypage/orders/sheet")
					.param("matchId", "1")
					.param("seatIds", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("좌석 선점이 만료되었습니다."));
		}

		@Test
		@DisplayName("선점 정보가 없으면 404를 반환한다")
		void getOrderSheet_선점_미발견_404() throws Exception {
			given(orderService.getOrderSheet(any(), any(), any()))
				.willThrow(new CustomException(ErrorCode.SEAT_HOLD_NOT_FOUND));

			mockMvc.perform(get("/mypage/orders/sheet")
					.param("matchId", "1")
					.param("seatIds", "101"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("좌석 선점 정보를 찾을 수 없습니다."));
		}

		@Test
		@DisplayName("가격 정책이 없으면 404를 반환한다")
		void getOrderSheet_가격정책_미발견_404() throws Exception {
			given(orderService.getOrderSheet(any(), any(), any()))
				.willThrow(new CustomException(ErrorCode.PRICE_POLICY_NOT_FOUND));

			mockMvc.perform(get("/mypage/orders/sheet")
					.param("matchId", "1")
					.param("seatIds", "101"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("해당 좌석의 가격 정책을 찾을 수 없습니다."));
		}
	}

	@Nested
	@DisplayName("POST /mypage/orders — 주문 생성")
	class CreateOrder {

		@BeforeEach
		void setAuth() {
			setAuthentication(1L);
		}

		@Test
		@DisplayName("유효한 요청이면 201과 생성된 주문 정보를 반환한다")
		void createOrder_성공() throws Exception {
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(orderService.createOrder(eq(1L), any(OrderCreateRequest.class)))
				.willReturn(createMockOrderCreateResponse());

			mockMvc.perform(post("/mypage/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.data.orderId").value(1))
				.andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"))
				.andExpect(jsonPath("$.data.matchId").value(1))
				.andExpect(jsonPath("$.data.seatCount").value(1))
				.andExpect(jsonPath("$.data.totalAmount").value(24000))
				.andExpect(jsonPath("$.data.bookingFee").value(2000));
		}

		@Test
		@DisplayName("생년월일이 6자리가 아니면 400을 반환한다")
		void createOrder_생년월일_형식오류_400() throws Exception {
			OrderCreateRequest invalidRequest = new OrderCreateRequest(
				1L,
				List.of(new SeatOrderItem(101L, TicketType.ADULT)),
				"홍길동", "hong@test.com", "010-1234-5678",
				"19990831" // 8자리 — 유효하지 않음
			);

			mockMvc.perform(post("/mypage/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("이메일 형식이 잘못되면 400을 반환한다")
		void createOrder_이메일_형식오류_400() throws Exception {
			OrderCreateRequest invalidRequest = new OrderCreateRequest(
				1L,
				List.of(new SeatOrderItem(101L, TicketType.ADULT)),
				"홍길동", "not-an-email", "010-1234-5678", "990831"
			);

			mockMvc.perform(post("/mypage/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("seats가 비어있으면 400을 반환한다")
		void createOrder_빈좌석_400() throws Exception {
			OrderCreateRequest invalidRequest = new OrderCreateRequest(
				1L, List.of(),
				"홍길동", "hong@test.com", "010-1234-5678", "990831"
			);

			mockMvc.perform(post("/mypage/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("사용자가 없으면 404를 반환한다")
		void createOrder_사용자_미발견_404() throws Exception {
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(orderService.createOrder(any(), any()))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			mockMvc.perform(post("/mypage/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
		}

		@Test
		@DisplayName("선점이 만료된 경우 400을 반환한다")
		void createOrder_선점_만료_400() throws Exception {
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(orderService.createOrder(any(), any()))
				.willThrow(new CustomException(ErrorCode.SEAT_HOLD_EXPIRED));

			mockMvc.perform(post("/mypage/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("좌석 선점이 만료되었습니다."));
		}

		@Test
		@DisplayName("이미 주문된 좌석이면 400을 반환한다")
		void createOrder_이미_주문된_좌석_400() throws Exception {
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(orderService.createOrder(any(), any()))
				.willThrow(new CustomException(ErrorCode.INVALID_ORDER_STATUS));

			mockMvc.perform(post("/mypage/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("주문 상태가 올바르지 않습니다."));
		}

		@Test
		@DisplayName("선점 정보가 없으면 404를 반환한다")
		void createOrder_선점_미발견_404() throws Exception {
			OrderCreateRequest request = OrderFixture.createOrderCreateRequest();

			given(orderService.createOrder(any(), any()))
				.willThrow(new CustomException(ErrorCode.SEAT_HOLD_NOT_FOUND));

			mockMvc.perform(post("/mypage/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("좌석 선점 정보를 찾을 수 없습니다."));
		}

		@Test
		@DisplayName("matchId가 없으면 400을 반환한다")
		void createOrder_matchId_누락_400() throws Exception {
			String invalidJson = """
				{
				  "seats": [{"matchSeatId": 101, "ticketType": "ADULT"}],
				  "ordererName": "홍길동",
				  "ordererEmail": "hong@test.com",
				  "ordererPhone": "010-1234-5678",
				  "ordererBirthDate": "990831"
				}
				""";

			mockMvc.perform(post("/mypage/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(invalidJson))
				.andExpect(status().isBadRequest());
		}
	}
}
