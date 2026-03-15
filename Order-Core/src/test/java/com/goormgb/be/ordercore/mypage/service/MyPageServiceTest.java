package com.goormgb.be.ordercore.mypage.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.mypage.MyPageFixture;
import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.enums.TicketTab;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.OrderSeatRow;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.TicketRow;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.UserSns;
import com.goormgb.be.user.enums.SocialProvider;
import com.goormgb.be.user.repository.UserRepository;
import com.goormgb.be.user.repository.UserSnsRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyPageService 서비스 단위 테스트")
class MyPageServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserSnsRepository userSnsRepository;
	@Mock
	private OrderRepository orderRepository;
	@Mock
	private MyPageQueryService myPageQueryService;

	private MyPageService myPageService;

	@BeforeEach
	void setUp() {
		myPageService = new MyPageService(
			userRepository, userSnsRepository, orderRepository, myPageQueryService
		);
	}

	private UserSns createUserSns(User user) {
		return UserSns.builder()
			.user(user)
			.provider(SocialProvider.KAKAO)
			.providerUserId("kakao-12345")
			.build();
	}

	@Nested
	@DisplayName("getProfile — 프로필 요약 조회")
	class GetProfile {

		@Test
		@DisplayName("유효한 userId이면 프로필과 티켓 요약을 반환한다")
		void getProfile_성공() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			UserSns userSns = createUserSns(user);
			Instant now = Instant.now();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userSnsRepository.findByUserId(userId)).willReturn(Optional.of(userSns));
			given(orderRepository.countUpcomingOrders(eq(userId), any(), any())).willReturn(2L);
			given(orderRepository.countByUserIdAndStatusIn(eq(userId), any())).willReturn(1L);
			given(orderRepository.countCompletedOrders(eq(userId), any())).willReturn(5L);

			MyPageProfileResponse response = myPageService.getProfile(userId);

			assertThat(response).isNotNull();
			assertThat(response.profile().nickname()).isEqualTo("테스터");
			assertThat(response.profile().snsProvider()).isEqualTo("KAKAO");
			assertThat(response.ticketSummary().upcomingCount()).isEqualTo(2);
			assertThat(response.ticketSummary().cancelRefundCount()).isEqualTo(1);
			assertThat(response.ticketSummary().completedCount()).isEqualTo(5);
		}

		@Test
		@DisplayName("SNS 정보가 없으면 snsProvider는 null이다")
		void getProfile_SNS없음_null() {
			Long userId = 1L;
			User user = OrderFixture.createUser();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userSnsRepository.findByUserId(userId)).willReturn(Optional.empty());
			given(orderRepository.countUpcomingOrders(eq(userId), any(), any())).willReturn(0L);
			given(orderRepository.countByUserIdAndStatusIn(eq(userId), any())).willReturn(0L);
			given(orderRepository.countCompletedOrders(eq(userId), any())).willReturn(0L);

			MyPageProfileResponse response = myPageService.getProfile(userId);

			assertThat(response.profile().snsProvider()).isNull();
		}

		@Test
		@DisplayName("존재하지 않는 userId이면 USER_NOT_FOUND 예외가 발생한다")
		void getProfile_사용자_미발견_예외() {
			Long userId = 999L;

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			assertThatThrownBy(() -> myPageService.getProfile(userId))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("모든 카운트가 0이어도 정상 반환한다")
		void getProfile_카운트_모두_0() {
			Long userId = 1L;
			User user = OrderFixture.createUser();

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userSnsRepository.findByUserId(userId)).willReturn(Optional.empty());
			given(orderRepository.countUpcomingOrders(eq(userId), any(), any())).willReturn(0L);
			given(orderRepository.countByUserIdAndStatusIn(eq(userId), any())).willReturn(0L);
			given(orderRepository.countCompletedOrders(eq(userId), any())).willReturn(0L);

			MyPageProfileResponse response = myPageService.getProfile(userId);

			assertThat(response.ticketSummary().upcomingCount()).isEqualTo(0);
			assertThat(response.ticketSummary().cancelRefundCount()).isEqualTo(0);
			assertThat(response.ticketSummary().completedCount()).isEqualTo(0);
		}
	}

	@Nested
	@DisplayName("getTickets — 예매 내역 목록 조회")
	class GetTickets {

		@Test
		@DisplayName("BOOKED 탭으로 티켓 목록을 반환한다")
		void getTickets_BOOKED_성공() {
			Long userId = 1L;
			TicketRow ticketRow = MyPageFixture.createTicketRow(101L, OrderStatus.PAID);
			OrderSeatRow seatRow = MyPageFixture.createOrderSeatRow(101L);

			given(orderRepository.countByUserId(userId)).willReturn(8L);
			given(orderRepository.countUpcomingOrders(eq(userId), any(), any())).willReturn(2L);
			given(orderRepository.countByUserIdAndStatusIn(eq(userId), any())).willReturn(1L);
			given(orderRepository.countCompletedOrders(eq(userId), any())).willReturn(5L);
			given(myPageQueryService.countTickets(eq(userId), any())).willReturn(3L);
			given(myPageQueryService.findTickets(eq(userId), any(), eq(0), eq(10))).willReturn(List.of(ticketRow));
			given(myPageQueryService.findOrderSeatRowsByOrderIds(List.of(101L))).willReturn(List.of(seatRow));

			MyPageTicketListResponse response = myPageService.getTickets(userId, "BOOKED", 0, 10);

			assertThat(response).isNotNull();
			assertThat(response.currentTab()).isEqualTo("BOOKED");
			assertThat(response.tickets()).hasSize(1);
			assertThat(response.tickets().get(0).ticketId()).isEqualTo(101L);
			assertThat(response.tickets().get(0).status()).isEqualTo(OrderStatus.PAID);
			assertThat(response.tickets().get(0).actions().canCancel()).isTrue();
			assertThat(response.tickets().get(0).actions().canViewDetail()).isTrue();
			assertThat(response.tickets().get(0).seats()).hasSize(1);
		}

		@Test
		@DisplayName("CANCEL_REFUND 탭으로 취소/환불 티켓 목록을 반환한다")
		void getTickets_CANCEL_REFUND_성공() {
			Long userId = 1L;
			TicketRow cancelledRow = MyPageFixture.createPastTicketRow(102L, OrderStatus.CANCELLED);

			given(orderRepository.countByUserId(userId)).willReturn(5L);
			given(orderRepository.countUpcomingOrders(eq(userId), any(), any())).willReturn(0L);
			given(orderRepository.countByUserIdAndStatusIn(eq(userId), any())).willReturn(0L);
			given(orderRepository.countCompletedOrders(eq(userId), any())).willReturn(3L);
			given(myPageQueryService.countTickets(eq(userId), any())).willReturn(2L);
			given(myPageQueryService.findTickets(eq(userId), any(), eq(0), eq(10))).willReturn(List.of(cancelledRow));
			given(myPageQueryService.findOrderSeatRowsByOrderIds(List.of(102L))).willReturn(Collections.emptyList());

			MyPageTicketListResponse response = myPageService.getTickets(userId, "CANCEL_REFUND", 0, 10);

			assertThat(response.currentTab()).isEqualTo("CANCEL_REFUND");
			assertThat(response.tickets()).hasSize(1);
			assertThat(response.tickets().get(0).status()).isEqualTo(OrderStatus.CANCELLED);
			assertThat(response.tickets().get(0).actions().canDeposit()).isFalse();
			assertThat(response.tickets().get(0).actions().canCancel()).isFalse();
		}

		@Test
		@DisplayName("PAYMENT_PENDING 상태 티켓은 canDeposit이 true이다")
		void getTickets_PAYMENT_PENDING_canDeposit_true() {
			Long userId = 1L;
			TicketRow pendingRow = MyPageFixture.createTicketRow(103L, OrderStatus.PAYMENT_PENDING);

			given(orderRepository.countByUserId(userId)).willReturn(1L);
			given(orderRepository.countUpcomingOrders(eq(userId), any(), any())).willReturn(1L);
			given(orderRepository.countByUserIdAndStatusIn(eq(userId), any())).willReturn(0L);
			given(orderRepository.countCompletedOrders(eq(userId), any())).willReturn(0L);
			given(myPageQueryService.countTickets(eq(userId), any())).willReturn(1L);
			given(myPageQueryService.findTickets(eq(userId), any(), eq(0), eq(10))).willReturn(List.of(pendingRow));
			given(myPageQueryService.findOrderSeatRowsByOrderIds(List.of(103L))).willReturn(Collections.emptyList());

			MyPageTicketListResponse response = myPageService.getTickets(userId, "BOOKED", 0, 10);

			assertThat(response.tickets().get(0).actions().canDeposit()).isTrue();
			assertThat(response.tickets().get(0).actions().canCancel()).isFalse();
		}

		@Test
		@DisplayName("티켓이 없으면 빈 목록과 페이지네이션 정보를 반환한다")
		void getTickets_빈목록_반환() {
			Long userId = 1L;

			given(orderRepository.countByUserId(userId)).willReturn(0L);
			given(orderRepository.countUpcomingOrders(eq(userId), any(), any())).willReturn(0L);
			given(orderRepository.countByUserIdAndStatusIn(eq(userId), any())).willReturn(0L);
			given(orderRepository.countCompletedOrders(eq(userId), any())).willReturn(0L);
			given(myPageQueryService.countTickets(eq(userId), any())).willReturn(0L);
			given(myPageQueryService.findTickets(eq(userId), any(), eq(0), eq(10))).willReturn(Collections.emptyList());

			MyPageTicketListResponse response = myPageService.getTickets(userId, "BOOKED", 0, 10);

			assertThat(response.tickets()).isEmpty();
			assertThat(response.pagination().totalElements()).isEqualTo(0L);
			assertThat(response.pagination().totalPages()).isEqualTo(0);
			assertThat(response.pagination().hasNext()).isFalse();
		}

		@Test
		@DisplayName("페이지네이션 hasNext가 올바르게 계산된다")
		void getTickets_페이지네이션_hasNext_계산() {
			Long userId = 1L;
			TicketRow row1 = MyPageFixture.createTicketRow(101L, OrderStatus.PAID);
			TicketRow row2 = MyPageFixture.createTicketRow(102L, OrderStatus.PAID);

			given(orderRepository.countByUserId(userId)).willReturn(15L);
			given(orderRepository.countUpcomingOrders(eq(userId), any(), any())).willReturn(5L);
			given(orderRepository.countByUserIdAndStatusIn(eq(userId), any())).willReturn(0L);
			given(orderRepository.countCompletedOrders(eq(userId), any())).willReturn(10L);
			given(myPageQueryService.countTickets(eq(userId), any())).willReturn(15L);
			given(myPageQueryService.findTickets(eq(userId), any(), eq(0), eq(2))).willReturn(List.of(row1, row2));
			given(myPageQueryService.findOrderSeatRowsByOrderIds(any())).willReturn(Collections.emptyList());

			MyPageTicketListResponse response = myPageService.getTickets(userId, "BOOKED", 0, 2);

			assertThat(response.pagination().page()).isEqualTo(0);
			assertThat(response.pagination().size()).isEqualTo(2);
			assertThat(response.pagination().totalElements()).isEqualTo(15L);
			assertThat(response.pagination().totalPages()).isEqualTo(8);
			assertThat(response.pagination().hasNext()).isTrue();
		}

		@Test
		@DisplayName("size가 10을 초과하면 INVALID_PAGE_SIZE 예외가 발생한다")
		void getTickets_size초과_예외() {
			assertThatThrownBy(() -> myPageService.getTickets(1L, "BOOKED", 0, 11))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.INVALID_PAGE_SIZE.getMessage());
		}

		@Test
		@DisplayName("유효하지 않은 탭 값이면 INVALID_TICKET_TAB 예외가 발생한다")
		void getTickets_잘못된탭_예외() {
			assertThatThrownBy(() -> myPageService.getTickets(1L, "INVALID_TAB", 0, 10))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.INVALID_TICKET_TAB.getMessage());
		}

		@Test
		@DisplayName("summary는 탭과 무관하게 전체 통계를 반환한다")
		void getTickets_summary_탭무관_전체통계() {
			Long userId = 1L;

			given(orderRepository.countByUserId(userId)).willReturn(10L);
			given(orderRepository.countUpcomingOrders(eq(userId), any(), any())).willReturn(3L);
			given(orderRepository.countByUserIdAndStatusIn(eq(userId), any())).willReturn(2L);
			given(orderRepository.countCompletedOrders(eq(userId), any())).willReturn(5L);
			given(myPageQueryService.countTickets(eq(userId), any())).willReturn(0L);
			given(myPageQueryService.findTickets(eq(userId), any(), eq(0), eq(10))).willReturn(Collections.emptyList());

			MyPageTicketListResponse response = myPageService.getTickets(userId, "CANCEL_REFUND", 0, 10);

			assertThat(response.summary().totalCount()).isEqualTo(10);
			assertThat(response.summary().upcomingCount()).isEqualTo(3);
			assertThat(response.summary().cancelProcessingCount()).isEqualTo(2);
			assertThat(response.summary().completedCount()).isEqualTo(5);
		}
	}
}
