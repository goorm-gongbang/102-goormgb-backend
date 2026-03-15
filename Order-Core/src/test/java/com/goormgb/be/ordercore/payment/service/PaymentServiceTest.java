package com.goormgb.be.ordercore.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
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
	private PaymentRepository paymentRepository;
	@Mock
	private CashReceiptRepository cashReceiptRepository;

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentService(orderRepository, paymentRepository, cashReceiptRepository);
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

			given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
			given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

			PaymentProcessResponse response = paymentService.processPayment(userId, orderId, request);

			assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.TOSS_PAY);
			assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
			assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
			assertThat(response.paidAt()).isNotNull();
			assertThat(response.virtualAccount()).isNull();
		}

		@Test
		@DisplayName("카카오페이 결제 시 즉시 COMPLETED 상태가 되고 orderStatus가 PAID가 된다")
		void processPayment_KAKAO_PAY_즉시완료() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			PaymentProcessRequest request = PaymentFixture.createKakaoPayRequest();

			given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
			given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

			PaymentProcessResponse response = paymentService.processPayment(userId, orderId, request);

			assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.KAKAO_PAY);
			assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
			assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
		}

		@Test
		@DisplayName("가상계좌 결제 시 PENDING 상태를 유지하고 가상계좌 정보를 반환한다")
		void processPayment_VIRTUAL_ACCOUNT_대기상태() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			ReflectionTestUtils.setField(order, "id", orderId);
			PaymentProcessRequest request = PaymentFixture.createVirtualAccountRequest();

			given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
			given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

			PaymentProcessResponse response = paymentService.processPayment(userId, orderId, request);

			assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.VIRTUAL_ACCOUNT);
			assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
			assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
			assertThat(response.virtualAccount()).isNotNull();
			assertThat(response.virtualAccount().bank()).isEqualTo("국민은행");
			assertThat(response.virtualAccount().holder()).isEqualTo("구름GB");
			assertThat(response.virtualAccount().accountNumber()).startsWith("047-000-");
			assertThat(response.virtualAccount().depositDeadline()).isNotNull();
		}

		@Test
		@DisplayName("가상계좌 번호는 orderId 기반으로 생성된다")
		void processPayment_가상계좌번호_orderId_기반() {
			Long userId = 1L;
			Long orderId = 42L;
			Order order = createOrderWithUser(orderId, userId);
			PaymentProcessRequest request = PaymentFixture.createVirtualAccountRequest();

			given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
			given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

			PaymentProcessResponse response = paymentService.processPayment(userId, orderId, request);

			assertThat(response.virtualAccount().accountNumber()).isEqualTo("047-000-00000042");
		}

		@Test
		@DisplayName("주문이 없으면 ORDER_NOT_FOUND 예외가 발생한다")
		void processPayment_주문_미발견_예외() {
			given(orderRepository.findById(99L)).willReturn(Optional.empty());

			assertThatThrownBy(
				() -> paymentService.processPayment(1L, 99L, PaymentFixture.createTossPayRequest())
			)
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("주문 소유자가 아니면 ORDER_ACCESS_DENIED 예외가 발생한다")
		void processPayment_소유권_없음_예외() {
			Long actualOwnerId = 1L;
			Long attackerId = 99L;
			Order order = createOrderWithUser(1L, actualOwnerId);

			given(orderRepository.findById(1L)).willReturn(Optional.of(order));

			assertThatThrownBy(
				() -> paymentService.processPayment(attackerId, 1L, PaymentFixture.createTossPayRequest())
			)
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
		}

		@Test
		@DisplayName("PAYMENT_PENDING이 아닌 주문은 PAYMENT_ALREADY_COMPLETED 예외가 발생한다")
		void processPayment_이미_결제된_주문_예외() {
			Long userId = 1L;
			Order order = createOrderWithUser(1L, userId);
			order.updateStatus(OrderStatus.PAID); // PAID 상태로 변경

			given(orderRepository.findById(1L)).willReturn(Optional.of(order));

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
			Payment existingPayment = PaymentFixture.createVirtualAccountPayment(order);

			given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
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

			given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
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

			given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
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

			given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());

			assertThatThrownBy(
				() -> paymentService.createCashReceipt(userId, orderId, PaymentFixture.createPersonalDeductionRequest())
			)
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.PAYMENT_NOT_FOUND.getMessage());
		}

		@Test
		@DisplayName("현금영수증이 이미 신청된 경우 CASH_RECEIPT_ALREADY_EXISTS 예외가 발생한다")
		void createCashReceipt_중복신청_예외() {
			Long userId = 1L;
			Long orderId = 1L;
			Order order = createOrderWithUser(orderId, userId);
			Payment payment = PaymentFixture.createCompletedTossPayPayment(order);
			CashReceipt existing = PaymentFixture.createPersonalDeductionReceipt(payment);

			given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
			given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));
			given(cashReceiptRepository.findByPaymentId(payment.getId())).willReturn(Optional.of(existing));

			assertThatThrownBy(
				() -> paymentService.createCashReceipt(userId, orderId, PaymentFixture.createPersonalDeductionRequest())
			)
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.CASH_RECEIPT_ALREADY_EXISTS.getMessage());
		}

		@Test
		@DisplayName("주문 소유자가 아니면 ORDER_ACCESS_DENIED 예외가 발생한다")
		void createCashReceipt_소유권_없음_예외() {
			Long actualOwnerId = 1L;
			Long attackerId = 99L;
			Order order = createOrderWithUser(1L, actualOwnerId);

			given(orderRepository.findById(1L)).willReturn(Optional.of(order));

			assertThatThrownBy(
				() -> paymentService.createCashReceipt(attackerId, 1L, PaymentFixture.createPersonalDeductionRequest())
			)
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ORDER_ACCESS_DENIED.getMessage());
		}

		@Test
		@DisplayName("주문이 없으면 ORDER_NOT_FOUND 예외가 발생한다")
		void createCashReceipt_주문_미발견_예외() {
			given(orderRepository.findById(99L)).willReturn(Optional.empty());

			assertThatThrownBy(
				() -> paymentService.createCashReceipt(1L, 99L, PaymentFixture.createPersonalDeductionRequest())
			)
				.isInstanceOf(CustomException.class)
				.hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
		}
	}
}
