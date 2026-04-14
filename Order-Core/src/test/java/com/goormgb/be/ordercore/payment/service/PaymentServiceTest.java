package com.goormgb.be.ordercore.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
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

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.fixture.payment.PaymentFixture;
import com.goormgb.be.ordercore.metrics.OrderMetricsService;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.order.repository.OrderSeatRepository;
import com.goormgb.be.ordercore.payment.event.PaymentEventPublisher;
import com.goormgb.be.ordercore.payment.dto.request.CashReceiptCreateRequest;
import com.goormgb.be.ordercore.payment.dto.request.PaymentProcessRequest;
import com.goormgb.be.ordercore.payment.dto.response.CashReceiptCreateResponse;
import com.goormgb.be.ordercore.payment.dto.response.PaymentProcessResponse;
import com.goormgb.be.ordercore.payment.entity.CashReceipt;
import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.enums.CashReceiptPurpose;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;
import com.goormgb.be.ordercore.payment.enums.PaymentStatus;
import com.goormgb.be.ordercore.payment.repository.CashReceiptRepository;
import com.goormgb.be.ordercore.payment.repository.PaymentRepository;
import com.goormgb.be.user.entity.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 서비스 단위 테스트")
class PaymentServiceTest {

	@Mock
	private OrderRepository orderRepository;
	@Mock
	private OrderSeatRepository orderSeatRepository;
	@Mock
	private PaymentRepository paymentRepository;
	@Mock
	private CashReceiptRepository cashReceiptRepository;
	@Mock
	private OrderMetricsService orderMetricsService;
	@Mock
	private PaymentEventPublisher paymentEventPublisher;

	private PaymentService paymentService;
	private Clock clock;

	@BeforeEach
	void setUp() {
		// 경기(2026-03-11 09:30 UTC)보다 이전 시점으로 고정
		clock = Clock.fixed(Instant.parse("2026-03-04T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		paymentService = new PaymentService(clock, orderMetricsService, orderRepository, orderSeatRepository,
				paymentRepository, cashReceiptRepository, paymentEventPublisher);
	}

	private Order createOrderWithUser(Long orderId, Long userId) {
		User user = OrderFixture.createUserWithId(userId);
		Order order = OrderFixture.createOrder(user, OrderFixture.createWeekdayMatch());
		ReflectionTestUtils.setField(order, "id", orderId);
		return order;
	}

	@Nested
	@DisplayName("processPayment — 결제 처리")
	class ProcessPayment {

		@Test
		@DisplayName("토스페이 결제 시 즉시 COMPLETED 상태가 되고 orderStatus가 PAID가 된다")
		void processPayment_TOSS_PAY_즉시완료() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			PaymentProcessRequest request = PaymentFixture.createTossPayRequest();

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
			given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
			given(orderSeatRepository.findMatchSeatIdsByOrderId(orderId)).willReturn(java.util.List.of());

			PaymentProcessResponse response = paymentService.processPayment(userId, orderId, request);

			assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.TOSS_PAY);
			assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
			assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
			assertThat(response.paidAt()).isNotNull();
			assertThat(response.account()).isNull();
		}

		@Test
		@DisplayName("카카오페이 결제 시 즉시 COMPLETED 상태가 되고 orderStatus가 PAID가 된다")
		void processPayment_KAKAO_PAY_즉시완료() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			PaymentProcessRequest request = PaymentFixture.createKakaoPayRequest();

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
			given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
			given(orderSeatRepository.findMatchSeatIdsByOrderId(orderId)).willReturn(java.util.List.of());

			PaymentProcessResponse response = paymentService.processPayment(userId, orderId, request);

			assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.KAKAO_PAY);
			assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
			assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
		}

		@Test
		@DisplayName("무통장 입금 시 PENDING 상태를 유지하고 계좌 정보를 반환한다")
		void processPayment_BANK_TRANSFER_대기상태() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			ReflectionTestUtils.setField(order, "id", orderId);
			PaymentProcessRequest request = PaymentFixture.createBankTransferRequest();

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
			given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
			given(orderSeatRepository.findMatchSeatIdsByOrderId(orderId)).willReturn(List.of());

			PaymentProcessResponse response = paymentService.processPayment(userId, orderId, request);

			assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
			assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
			assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
			assertThat(response.account()).isNotNull();
			assertThat(response.account().bank()).isEqualTo("신한은행");
			assertThat(response.account().holder()).isEqualTo("주식회사 구름공방");
			assertThat(response.account().accountNumber()).isEqualTo("110-123-456789");
			assertThat(response.account().depositDeadline()).isNotNull();
		}

		@Test
		@DisplayName("간편결제 시 결제 완료 이벤트를 발행한다")
		void processPayment_간편결제_이벤트발행() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			List<Long> matchSeatIds = List.of(101L, 102L);
			PaymentProcessRequest request = PaymentFixture.createTossPayRequest();

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
			given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
			given(orderSeatRepository.findMatchSeatIdsByOrderId(orderId)).willReturn(matchSeatIds);

			paymentService.processPayment(userId, orderId, request);

			then(paymentEventPublisher).should().publishPaymentCompleted(eq(order), eq(matchSeatIds), eq("TOSS_PAY"));
		}

		@Test
		@DisplayName("무통장 입금 시에도 좌석 SOLD 전환 이벤트를 발행한다 (스케줄러가 BLOCKED를 풀지 못하도록)")
		void processPayment_무통장입금_이벤트발행() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			PaymentProcessRequest request = PaymentFixture.createBankTransferRequest();

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
			given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
			given(orderSeatRepository.findMatchSeatIdsByOrderId(orderId)).willReturn(List.of(101L));

			paymentService.processPayment(userId, orderId, request);

			then(paymentEventPublisher).should().publishPaymentCompleted(eq(order), eq(List.of(101L)), eq("BANK_TRANSFER"));
		}

		@Test
		@DisplayName("경기 시작 3시간 이내에 무통장 입금을 선택하면 BANK_TRANSFER_NOT_AVAILABLE 예외가 발생한다")
		void processPayment_무통장입금_3시간이내_차단() {
			Long userId = 1L;
			Long orderId = 1L;

			// 경기 시작 2시간 전으로 Clock 고정
			Instant matchAt = Instant.parse("2026-03-11T09:30:00Z");
			Clock nearMatchClock = Clock.fixed(matchAt.minus(Duration.ofHours(2)), ZoneId.of("Asia/Seoul"));
			PaymentService nearMatchPaymentService = new PaymentService(nearMatchClock, orderMetricsService,
					orderRepository, orderSeatRepository, paymentRepository, cashReceiptRepository,
					paymentEventPublisher);

			Order order = createOrderWithUser(orderId, userId);
			PaymentProcessRequest request = PaymentFixture.createBankTransferRequest();

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());

			assertThatThrownBy(
					() -> nearMatchPaymentService.processPayment(userId, orderId, request)
			)
					.isInstanceOf(CustomException.class)
					.hasMessage(ErrorCode.BANK_TRANSFER_NOT_AVAILABLE.getMessage());
		}

		@Test
		@DisplayName("주문이 없으면 ORDER_NOT_FOUND 예외가 발생한다")
		void processPayment_주문_미발견_예외() {
			given(orderRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

			assertThatThrownBy(
					() -> paymentService.processPayment(1L, 99L, PaymentFixture.createTossPayRequest())
			)
					.isInstanceOf(CustomException.class)
					.hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("주문 소유자가 아니면 ORDER_NOT_FOUND 예외가 발생한다")
		void processPayment_소유권_없음_예외() {
			Long actualOwnerId = 1L;
			Long attackerId = 99L;
			Order order = createOrderWithUser(1L, actualOwnerId);

			given(orderRepository.findByIdAndUserId(1L, attackerId)).willReturn(Optional.empty());

			assertThatThrownBy(
					() -> paymentService.processPayment(attackerId, 1L, PaymentFixture.createTossPayRequest())
			)
					.isInstanceOf(CustomException.class)
					.hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("PAYMENT_PENDING이 아닌 주문은 PAYMENT_ALREADY_COMPLETED 예외가 발생한다")
		void processPayment_이미_결제된_주문_예외() {
			Long userId = 1L;
			Order order = createOrderWithUser(1L, userId);
			order.updateStatus(OrderStatus.PAID); // PAID 상태로 변경

			given(orderRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(order));

			assertThatThrownBy(
					() -> paymentService.processPayment(userId, 1L, PaymentFixture.createTossPayRequest())
			)
					.isInstanceOf(CustomException.class)
					.hasMessage(ErrorCode.PAYMENT_ALREADY_COMPLETED.getMessage());
		}

		@Test
		@DisplayName("이미 결제 정보가 존재하면 PAYMENT_ALREADY_COMPLETED 예외가 발생한다")
		void processPayment_결제정보_이미_존재_예외() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			Payment existingPayment = PaymentFixture.createBankTransferPayment(order);

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(existingPayment));

			assertThatThrownBy(
					() -> paymentService.processPayment(userId, orderId, PaymentFixture.createTossPayRequest())
			)
					.isInstanceOf(CustomException.class)
					.hasMessage(ErrorCode.PAYMENT_ALREADY_COMPLETED.getMessage());
		}
	}

	@Nested
	@DisplayName("createCashReceipt — 현금영수증 신청")
	class CreateCashReceipt {

		@Test
		@DisplayName("개인소득공제 현금영수증을 정상 신청한다")
		void createCashReceipt_개인소득공제_성공() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			Payment payment = PaymentFixture.createCompletedTossPayPayment(order);
			CashReceiptCreateRequest request = PaymentFixture.createPersonalDeductionRequest();

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));
			given(cashReceiptRepository.findByPaymentId(payment.getId())).willReturn(Optional.empty());
			given(cashReceiptRepository.save(any(CashReceipt.class))).willAnswer(inv -> inv.getArgument(0));

			CashReceiptCreateResponse response = paymentService.createCashReceipt(userId, orderId, request);

			assertThat(response.orderId()).isEqualTo(orderId);
			assertThat(response.purpose()).isEqualTo(CashReceiptPurpose.PERSONAL_DEDUCTION);
			assertThat(response.number()).isEqualTo("010-1234-5678");
		}

		@Test
		@DisplayName("사업자지출증빙 현금영수증을 정상 신청한다")
		void createCashReceipt_사업자지출증빙_성공() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			Payment payment = PaymentFixture.createCompletedTossPayPayment(order);
			CashReceiptCreateRequest request = PaymentFixture.createBusinessExpenseRequest();

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));
			given(cashReceiptRepository.findByPaymentId(payment.getId())).willReturn(Optional.empty());
			given(cashReceiptRepository.save(any(CashReceipt.class))).willAnswer(inv -> inv.getArgument(0));

			CashReceiptCreateResponse response = paymentService.createCashReceipt(userId, orderId, request);

			assertThat(response.purpose()).isEqualTo(CashReceiptPurpose.BUSINESS_EXPENSE);
			assertThat(response.number()).isEqualTo("123-45-67890");
		}

		@Test
		@DisplayName("결제 정보가 없으면 PAYMENT_NOT_FOUND 예외가 발생한다")
		void createCashReceipt_결제_미발견_예외() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());

			assertThatThrownBy(
					() -> paymentService.createCashReceipt(userId, orderId,
							PaymentFixture.createPersonalDeductionRequest())
			)
					.isInstanceOf(CustomException.class)
					.hasMessage(ErrorCode.PAYMENT_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("현금영수증이 이미 존재하면 기존 정보를 수정한다")
		void createCashReceipt_기존_정보_수정() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			Payment payment = PaymentFixture.createCompletedTossPayPayment(order);
			CashReceipt existing = PaymentFixture.createPersonalDeductionReceipt(payment);

			given(orderRepository.findByIdAndUserId(orderId, userId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));
			given(cashReceiptRepository.findByPaymentId(payment.getId())).willReturn(Optional.of(existing));

			CashReceiptCreateResponse response = paymentService.createCashReceipt(userId, orderId,
					PaymentFixture.createBusinessExpenseRequest());

			assertThat(response.orderId()).isEqualTo(orderId);
			assertThat(response.purpose()).isEqualTo(CashReceiptPurpose.BUSINESS_EXPENSE);
			assertThat(response.number()).isEqualTo("123-45-67890");
		}

		@Test
		@DisplayName("주문 소유자가 아니면 ORDER_NOT_FOUND 예외가 발생한다")
		void createCashReceipt_소유권_없음_예외() {
			Long actualOwnerId = 1L;
			Long attackerId = 99L;
			Order order = createOrderWithUser(1L, actualOwnerId);

			given(orderRepository.findByIdAndUserId(1L, attackerId)).willReturn(Optional.empty());

			assertThatThrownBy(
					() -> paymentService.createCashReceipt(attackerId, 1L,
							PaymentFixture.createPersonalDeductionRequest())
			)
					.isInstanceOf(CustomException.class)
					.hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("주문이 없으면 ORDER_NOT_FOUND 예외가 발생한다")
		void createCashReceipt_주문_미발견_예외() {
			given(orderRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

			assertThatThrownBy(
					() -> paymentService.createCashReceipt(1L, 99L, PaymentFixture.createPersonalDeductionRequest())
			)
					.isInstanceOf(CustomException.class)
					.hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
		}
	}
}
