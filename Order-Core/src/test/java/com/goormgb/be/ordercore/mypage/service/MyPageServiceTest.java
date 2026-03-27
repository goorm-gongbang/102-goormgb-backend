package com.goormgb.be.ordercore.mypage.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.cancellation.entity.CancellationFeePolicy;
import com.goormgb.be.ordercore.cancellation.repository.CancellationFeePolicyRepository;
import com.goormgb.be.ordercore.fixture.mypage.MyPageFixture;
import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.mypage.dto.request.MyPageAccountUpdateRequest;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageAccountResponse;
import com.goormgb.be.ordercore.mypage.dto.query.TicketDetailBaseRow;
import com.goormgb.be.ordercore.mypage.dto.query.TicketSeatDetailRow;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketCancelResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketDetailResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketQrResponse;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.OrderSeatRow;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.TicketRow;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.qrtoken.entity.QrToken;
import com.goormgb.be.ordercore.qrtoken.repository.QrTokenRepository;
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
	@Mock
	private CancellationFeePolicyRepository cancellationFeePolicyRepository;
	@Mock
	private QrTokenRepository qrTokenRepository;

	private MyPageService myPageService;
	private Clock clock;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2026-03-26T00:00:00Z"), ZoneOffset.UTC);
		myPageService = new MyPageService(
			userRepository,
			userSnsRepository,
			orderRepository,
			qrTokenRepository,
			myPageQueryService,
			cancellationFeePolicyRepository,
			clock
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
	@DisplayName("updateAccount — 개인정보 수정")
	class UpdateAccount {

		@Test
		@DisplayName("유효한 닉네임이면 계정 정보가 수정된다")
		void updateAccount_성공() {
			Long userId = 1L;
			User user = OrderFixture.createUser();
			UserSns userSns = createUserSns(user);

			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND)).willReturn(user);
			given(userSnsRepository.findByUserId(userId)).willReturn(Optional.of(userSns));

			MyPageAccountResponse response = myPageService.updateAccount(
				userId,
				new MyPageAccountUpdateRequest("  goorm_new  ")
			);

			assertThat(response.nickname()).isEqualTo("goorm_new");
			assertThat(response.email()).isEqualTo("test@test.com");
			assertThat(response.snsAccounts()).hasSize(1);
			assertThat(response.snsAccounts().get(0).provider()).isEqualTo("KAKAO");
			assertThat(user.getNickname()).isEqualTo("goorm_new");
		}

		@Test
		@DisplayName("닉네임이 공백이면 INVALID_NICKNAME 예외가 발생한다")
		void updateAccount_닉네임공백_예외() {
			assertThatThrownBy(() -> myPageService.updateAccount(1L, new MyPageAccountUpdateRequest("   ")))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.INVALID_NICKNAME.getMessage());
		}

		@Test
		@DisplayName("닉네임이 20자를 초과하면 INVALID_NICKNAME 예외가 발생한다")
		void updateAccount_닉네임길이초과_예외() {
			assertThatThrownBy(() -> myPageService.updateAccount(1L, new MyPageAccountUpdateRequest("abcdefghijklmnopqrstu")))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.INVALID_NICKNAME.getMessage());
		}

		@Test
		@DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
		void updateAccount_사용자없음_예외() {
			Long userId = 999L;
			given(userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND))
				.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

			assertThatThrownBy(() -> myPageService.updateAccount(userId, new MyPageAccountUpdateRequest("goorm_new")))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
		}
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

	@Nested
	@DisplayName("getTicketDetail — 예매 상세 조회")
	class GetTicketDetail {

		@Test
		@DisplayName("PAID 상태 상세 정보를 반환한다")
		void getTicketDetail_PAID_성공() {
			Long userId = 1L;
			Long ticketId = 101L;
			TicketDetailBaseRow baseRow = MyPageFixture.createTicketDetailBaseRow(ticketId, OrderStatus.PAID);
			List<TicketSeatDetailRow> seatRows = MyPageFixture.createTicketSeatDetailRows();
			CancellationFeePolicy policy = CancellationFeePolicy.builder()
				.daysBeforeMatchMin(1)
				.daysBeforeMatchMax(6)
				.cancellable(true)
				.ticketFeeRate(new BigDecimal("0.100"))
				.bookingFeeRefundable(false)
				.build();

			given(myPageQueryService.findTicketDetailBaseByOrderId(ticketId)).willReturn(Optional.of(baseRow));
			given(myPageQueryService.findTicketSeatRowsByOrderId(ticketId)).willReturn(seatRows);
			given(cancellationFeePolicyRepository.findByDaysLeft(anyInt())).willReturn(Optional.of(policy));

			MyPageTicketDetailResponse response = myPageService.getTicketDetail(userId, ticketId);

			assertThat(response.ticketId()).isEqualTo(ticketId);
			assertThat(response.status()).isEqualTo(OrderStatus.PAID);
			assertThat(response.payment().paymentMethod()).isEqualTo("TOSS_PAY");
			assertThat(response.virtualAccount()).isNull();
			assertThat(response.cancellation()).isNull();
			assertThat(response.actions().canPrint()).isTrue();
			assertThat(response.seats()).hasSize(2);
			assertThat(response.payment().cashReceipt()).isNotNull();
		}

		@Test
		@DisplayName("PAYMENT_PENDING 무통장 상세 조회 시 virtualAccount를 반환한다")
		void getTicketDetail_PAYMENT_PENDING_가상계좌_성공() {
			Long userId = 1L;
			Long ticketId = 102L;
			TicketDetailBaseRow baseRow = MyPageFixture.createTicketDetailBaseRow(ticketId,
				OrderStatus.PAYMENT_PENDING);
			CancellationFeePolicy policy = CancellationFeePolicy.builder()
				.daysBeforeMatchMin(7)
				.daysBeforeMatchMax(null)
				.cancellable(true)
				.ticketFeeRate(BigDecimal.ZERO)
				.bookingFeeRefundable(true)
				.build();

			given(myPageQueryService.findTicketDetailBaseByOrderId(ticketId)).willReturn(Optional.of(baseRow));
			given(myPageQueryService.findTicketSeatRowsByOrderId(ticketId))
				.willReturn(MyPageFixture.createTicketSeatDetailRows());
			given(cancellationFeePolicyRepository.findByDaysLeft(anyInt())).willReturn(Optional.of(policy));

			MyPageTicketDetailResponse response = myPageService.getTicketDetail(userId, ticketId);

			assertThat(response.status()).isEqualTo(OrderStatus.PAYMENT_PENDING);
			assertThat(response.payment().paymentMethod()).isEqualTo("VIRTUAL_ACCOUNT");
			assertThat(response.payment().paidAt()).isNull();
			assertThat(response.virtualAccount()).isNotNull();
			assertThat(response.actions().canPrint()).isFalse();
		}

		@Test
		@DisplayName("취소된 티켓은 cancellation 정보를 반환한다")
		void getTicketDetail_CANCELLED_성공() {
			Long userId = 1L;
			Long ticketId = 103L;
			TicketDetailBaseRow baseRow = MyPageFixture.createTicketDetailBaseRow(ticketId, OrderStatus.CANCELLED);
			CancellationFeePolicy policy = CancellationFeePolicy.builder()
				.daysBeforeMatchMin(1)
				.daysBeforeMatchMax(6)
				.cancellable(true)
				.ticketFeeRate(new BigDecimal("0.100"))
				.bookingFeeRefundable(false)
				.build();

			given(myPageQueryService.findTicketDetailBaseByOrderId(ticketId)).willReturn(Optional.of(baseRow));
			given(myPageQueryService.findTicketSeatRowsByOrderId(ticketId))
				.willReturn(MyPageFixture.createTicketSeatDetailRows());
			given(cancellationFeePolicyRepository.findByDaysLeft(anyInt())).willReturn(Optional.of(policy));

			MyPageTicketDetailResponse response = myPageService.getTicketDetail(userId, ticketId);

			assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
			assertThat(response.cancellation()).isNotNull();
			assertThat(response.actions().canPrint()).isFalse();
		}

		@Test
		@DisplayName("존재하지 않는 티켓이면 ORDER_NOT_FOUND 예외가 발생한다")
		void getTicketDetail_주문없음_예외() {
			Long ticketId = 999L;
			given(myPageQueryService.findTicketDetailBaseByOrderId(ticketId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> myPageService.getTicketDetail(1L, ticketId))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("본인 소유가 아닌 티켓이면 ORDER_ACCESS_DENIED 예외가 발생한다")
		void getTicketDetail_권한없음_예외() {
			Long ticketId = 104L;
			TicketDetailBaseRow baseRow = MyPageFixture.createTicketDetailBaseRow(2L, ticketId, OrderStatus.PAID);
			given(myPageQueryService.findTicketDetailBaseByOrderId(ticketId)).willReturn(Optional.of(baseRow));

			assertThatThrownBy(() -> myPageService.getTicketDetail(1L, ticketId))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
		}
	}

	@Nested
	@DisplayName("getTicketEntryQr — 입장용 QR 조회")
	class GetTicketEntryQr {

		private Order createOrder(Long ticketId, Long userId, OrderStatus status, Instant matchAt) {
			var stadium = OrderFixture.createStadium();
			var homeClub = OrderFixture.createHomeClub(stadium);
			var awayClub = OrderFixture.createAwayClub(stadium);
			Match match = Match.builder()
				.matchAt(matchAt)
				.homeClub(homeClub)
				.awayClub(awayClub)
				.stadium(stadium)
				.saleStatus(SaleStatus.ON_SALE)
				.build();
			ReflectionTestUtils.setField(match, "id", 55L);

			User user = OrderFixture.createUserWithId(userId);
			Order order = OrderFixture.createOrderWithId(ticketId, user, match, 42000);
			order.updateStatus(status);
			return order;
		}

		@Test
		@DisplayName("유효한 토큰이 있으면 기존 토큰을 반환한다")
		void getTicketEntryQr_existingToken_성공() {
			Long userId = 1L;
			Long ticketId = 101L;
			Order order = createOrder(ticketId, userId, OrderStatus.PAID, Instant.now(clock).plus(10, ChronoUnit.MINUTES));
			List<TicketSeatDetailRow> seatRows = MyPageFixture.createTicketSeatDetailRows();
			QrToken existing = QrToken.builder()
				.qrToken("existing-qr-token")
				.expiresAt(Instant.now(clock).plus(2, ChronoUnit.MINUTES))
				.build();

			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));
			given(qrTokenRepository.findByOrderIdAndExpiresAtAfter(eq(ticketId), any())).willReturn(Optional.of(existing));
			given(myPageQueryService.findTicketSeatRowsByOrderId(ticketId)).willReturn(seatRows);

			MyPageTicketQrResponse response = myPageService.getTicketEntryQr(userId, ticketId);

			assertThat(response.ticketId()).isEqualTo(ticketId);
			assertThat(response.qrToken()).isEqualTo("existing-qr-token");
			assertThat(response.seats()).hasSize(2);
		}

		@Test
		@DisplayName("유효한 토큰이 없으면 신규 UUID 토큰을 발급한다")
		void getTicketEntryQr_issueNewToken_성공() {
			Long userId = 1L;
			Long ticketId = 101L;
			Order order = createOrder(ticketId, userId, OrderStatus.PAID, Instant.now(clock).plus(10, ChronoUnit.MINUTES));
			List<TicketSeatDetailRow> seatRows = MyPageFixture.createTicketSeatDetailRows();

			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));
			given(qrTokenRepository.findByOrderIdAndExpiresAtAfter(eq(ticketId), any())).willReturn(Optional.empty());
			given(qrTokenRepository.save(any(QrToken.class))).willAnswer(invocation -> invocation.getArgument(0));
			given(myPageQueryService.findTicketSeatRowsByOrderId(ticketId)).willReturn(seatRows);

			MyPageTicketQrResponse response = myPageService.getTicketEntryQr(userId, ticketId);

			assertThat(response.ticketId()).isEqualTo(ticketId);
			assertThat(response.qrToken()).isNotBlank();
			assertThat(response.expiresAt()).isAfter(Instant.now(clock));
			then(qrTokenRepository).should().save(any(QrToken.class));
		}

		@Test
		@DisplayName("PAID 상태가 아니면 INVALID_ORDER_STATUS 예외가 발생한다")
		void getTicketEntryQr_notPaid_예외() {
			Long ticketId = 101L;
			Order order = createOrder(ticketId, 1L, OrderStatus.PAYMENT_PENDING, Instant.now(clock).plus(10, ChronoUnit.MINUTES));
			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));

			assertThatThrownBy(() -> myPageService.getTicketEntryQr(1L, ticketId))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());
		}

		@Test
		@DisplayName("입장 가능 시간 이전이면 ENTRY_QR_NOT_AVAILABLE_YET 예외가 발생한다")
		void getTicketEntryQr_beforeEntryWindow_예외() {
			Long ticketId = 101L;
			Order order = createOrder(ticketId, 1L, OrderStatus.PAID, Instant.now(clock).plus(5, ChronoUnit.HOURS));
			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));

			assertThatThrownBy(() -> myPageService.getTicketEntryQr(1L, ticketId))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ENTRY_QR_NOT_AVAILABLE_YET.getMessage());
		}

		@Test
		@DisplayName("경기 시작 이후면 ENTRY_QR_MATCH_STARTED 예외가 발생한다")
		void getTicketEntryQr_matchStarted_예외() {
			Long ticketId = 101L;
			Order order = createOrder(ticketId, 1L, OrderStatus.PAID, Instant.now(clock).minus(1, ChronoUnit.HOURS));
			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));

			assertThatThrownBy(() -> myPageService.getTicketEntryQr(1L, ticketId))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ENTRY_QR_MATCH_STARTED.getMessage());
		}
	}

	@Nested
	@DisplayName("requestTicketCancel — 티켓 취소 요청")
	class RequestTicketCancel {

		private Order createOrder(Long ticketId, Long userId, OrderStatus status, Instant matchAt) {
			var stadium = OrderFixture.createStadium();
			var homeClub = OrderFixture.createHomeClub(stadium);
			var awayClub = OrderFixture.createAwayClub(stadium);
			Match match = Match.builder()
				.matchAt(matchAt)
				.homeClub(homeClub)
				.awayClub(awayClub)
				.stadium(stadium)
				.saleStatus(SaleStatus.ON_SALE)
				.build();
			ReflectionTestUtils.setField(match, "id", 55L);

			User user = OrderFixture.createUserWithId(userId);
			Order order = OrderFixture.createOrderWithId(ticketId, user, match, 42000);
			order.updateStatus(status);
			return order;
		}

		@Test
		@DisplayName("D-1~D-6 정책이면 취소수수료는 bookingFee + 티켓금액 10% 이다")
		void requestTicketCancel_d1to6_성공() {
			Long userId = 1L;
			Long ticketId = 101L;
			Order order = createOrder(ticketId, userId, OrderStatus.PAID, Instant.now().plus(2, ChronoUnit.DAYS));
			CancellationFeePolicy policy = CancellationFeePolicy.builder()
				.daysBeforeMatchMin(1)
				.daysBeforeMatchMax(6)
				.cancellable(true)
				.ticketFeeRate(new BigDecimal("0.100"))
				.bookingFeeRefundable(false)
				.build();

			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));
			given(cancellationFeePolicyRepository.findByDaysLeft(anyInt())).willReturn(Optional.of(policy));

			MyPageTicketCancelResponse response = myPageService.requestTicketCancel(userId, ticketId);

			assertThat(response.ticketId()).isEqualTo(ticketId);
			assertThat(response.status()).isEqualTo(OrderStatus.CANCEL_REQUESTED);
			assertThat(response.totalAmount()).isEqualTo(42000);
			assertThat(response.cancellationFee()).isEqualTo(6000);
			assertThat(response.refundedAmount()).isEqualTo(36000);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);
			assertThat(order.getCancellationFee()).isEqualTo(6000);
			assertThat(order.getRefundedAmount()).isEqualTo(36000);
		}

		@Test
		@DisplayName("D-7 이상 정책이면 전액 환불한다")
		void requestTicketCancel_d7plus_전액환불() {
			Long userId = 1L;
			Long ticketId = 101L;
			Order order = createOrder(ticketId, userId, OrderStatus.PAID, Instant.now().plus(10, ChronoUnit.DAYS));
			CancellationFeePolicy policy = CancellationFeePolicy.builder()
				.daysBeforeMatchMin(7)
				.daysBeforeMatchMax(null)
				.cancellable(true)
				.ticketFeeRate(BigDecimal.ZERO)
				.bookingFeeRefundable(true)
				.build();

			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));
			given(cancellationFeePolicyRepository.findByDaysLeft(anyInt())).willReturn(Optional.of(policy));

			MyPageTicketCancelResponse response = myPageService.requestTicketCancel(userId, ticketId);

			assertThat(response.cancellationFee()).isEqualTo(0);
			assertThat(response.refundedAmount()).isEqualTo(42000);
		}

		@Test
		@DisplayName("D-7 이상이어도 예매 당일이 아니면 bookingFee는 환불하지 않는다")
		void requestTicketCancel_d7plus_notBookingDay_bookingFee미환불() {
			Long userId = 1L;
			Long ticketId = 101L;
			Order order = createOrder(ticketId, userId, OrderStatus.PAID, Instant.now().plus(10, ChronoUnit.DAYS));
			ReflectionTestUtils.setField(order, "createdAt", Instant.now().minus(1, ChronoUnit.DAYS));

			CancellationFeePolicy policy = CancellationFeePolicy.builder()
				.daysBeforeMatchMin(7)
				.daysBeforeMatchMax(null)
				.cancellable(true)
				.ticketFeeRate(BigDecimal.ZERO)
				.bookingFeeRefundable(true)
				.build();

			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));
			given(cancellationFeePolicyRepository.findByDaysLeft(anyInt())).willReturn(Optional.of(policy));

			MyPageTicketCancelResponse response = myPageService.requestTicketCancel(userId, ticketId);

			assertThat(response.cancellationFee()).isEqualTo(2000);
			assertThat(response.refundedAmount()).isEqualTo(40000);
		}

		@Test
		@DisplayName("취소 불가 정책이면 TICKET_CANCEL_NOT_ALLOWED 예외가 발생한다")
		void requestTicketCancel_notCancellable_예외() {
			Long ticketId = 101L;
			Order order = createOrder(ticketId, 1L, OrderStatus.PAID, Instant.now().plus(1, ChronoUnit.HOURS));
			CancellationFeePolicy policy = CancellationFeePolicy.builder()
				.daysBeforeMatchMin(0)
				.daysBeforeMatchMax(0)
				.cancellable(false)
				.ticketFeeRate(BigDecimal.ZERO)
				.bookingFeeRefundable(false)
				.build();

			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));
			given(cancellationFeePolicyRepository.findByDaysLeft(anyInt())).willReturn(Optional.of(policy));

			assertThatThrownBy(() -> myPageService.requestTicketCancel(1L, ticketId))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.TICKET_CANCEL_NOT_ALLOWED.getMessage());
		}

		@Test
		@DisplayName("PAID 상태가 아니면 INVALID_ORDER_STATUS 예외가 발생한다")
		void requestTicketCancel_notPaid_예외() {
			Long ticketId = 101L;
			Order order = createOrder(ticketId, 1L, OrderStatus.CANCELLED, Instant.now().plus(2, ChronoUnit.DAYS));
			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));

			assertThatThrownBy(() -> myPageService.requestTicketCancel(1L, ticketId))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());
		}

		@Test
		@DisplayName("본인 소유가 아니면 ORDER_ACCESS_DENIED 예외가 발생한다")
		void requestTicketCancel_권한없음_예외() {
			Long ticketId = 101L;
			Order order = createOrder(ticketId, 2L, OrderStatus.PAID, Instant.now().plus(2, ChronoUnit.DAYS));
			given(orderRepository.findByIdForUpdate(ticketId)).willReturn(Optional.of(order));

			assertThatThrownBy(() -> myPageService.requestTicketCancel(1L, ticketId))
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
		}
	}
}
