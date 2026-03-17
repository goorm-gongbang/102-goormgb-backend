package com.goormgb.be.ordercore.payment.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;
import com.goormgb.be.ordercore.payment.enums.PaymentStatus;

@DisplayName("Payment 엔티티 단위 테스트")
class PaymentEntityTest {

	private Order createOrder() {
		return OrderFixture.createOrder(
				OrderFixture.createUser(),
				OrderFixture.createWeekdayMatch()
		);
	}

	private Payment createPendingPayment(PaymentMethod method) {
		return Payment.builder()
				.order(createOrder())
				.paymentMethod(method)
				.build();
	}

	@Nested
	@DisplayName("생성 시 초기 상태 검증")
	class Creation {

		@Test
		@DisplayName("결제 생성 시 상태는 PENDING이다")
		void 생성시_상태는_PENDING() {
			Payment payment = createPendingPayment(PaymentMethod.TOSS_PAY);
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
		}

		@Test
		@DisplayName("결제 생성 시 paidAt은 null이다")
		void 생성시_paidAt은_null() {
			Payment payment = createPendingPayment(PaymentMethod.TOSS_PAY);
			assertThat(payment.getPaidAt()).isNull();
		}

		@Test
		@DisplayName("간편결제 생성 시 계좌 정보는 null이다")
		void 간편결제_생성시_계좌_null() {
			Payment payment = createPendingPayment(PaymentMethod.KAKAO_PAY);
			assertThat(payment.getAccountBank()).isNull();
			assertThat(payment.getAccountNumber()).isNull();
			assertThat(payment.getAccountHolder()).isNull();
			assertThat(payment.getDepositDeadline()).isNull();
		}

		@Test
		@DisplayName("무통장 입금 생성 시 계좌 정보가 올바르게 설정된다")
		void 무통장입금_생성시_계좌정보_설정() {
			Instant deadline = Instant.now().plus(3, ChronoUnit.DAYS);
			Payment payment = Payment.builder()
					.order(createOrder())
					.paymentMethod(PaymentMethod.BANK_TRANSFER)
					.accountBank("신한은행")
					.accountNumber("110-123-456789")
					.accountHolder("주식회사 구름공방")
					.depositDeadline(deadline)
					.build();

			assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
			assertThat(payment.getAccountBank()).isEqualTo("신한은행");
			assertThat(payment.getAccountNumber()).isEqualTo("110-123-456789");
			assertThat(payment.getAccountHolder()).isEqualTo("주식회사 구름공방");
			assertThat(payment.getDepositDeadline()).isEqualTo(deadline);
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
		}

		@Test
		@DisplayName("결제 수단이 올바르게 저장된다")
		void 결제수단_저장() {
			Payment tossPay = createPendingPayment(PaymentMethod.TOSS_PAY);
			Payment kakaoPay = createPendingPayment(PaymentMethod.KAKAO_PAY);
			Payment bankTransfer = createPendingPayment(PaymentMethod.BANK_TRANSFER);

			assertThat(tossPay.getPaymentMethod()).isEqualTo(PaymentMethod.TOSS_PAY);
			assertThat(kakaoPay.getPaymentMethod()).isEqualTo(PaymentMethod.KAKAO_PAY);
			assertThat(bankTransfer.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
		}
	}

	@Nested
	@DisplayName("complete() 결제 완료 처리")
	class Complete {

		@Test
		@DisplayName("complete() 호출 시 상태가 COMPLETED로 변경된다")
		void complete_상태가_COMPLETED() {
			Payment payment = createPendingPayment(PaymentMethod.TOSS_PAY);
			payment.complete();
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
		}

		@Test
		@DisplayName("complete() 호출 시 paidAt이 설정된다")
		void complete_paidAt_설정() {
			Payment payment = createPendingPayment(PaymentMethod.TOSS_PAY);
			Instant before = Instant.now();
			payment.complete();
			assertThat(payment.getPaidAt()).isNotNull();
			assertThat(payment.getPaidAt()).isAfterOrEqualTo(before);
		}

		@Test
		@DisplayName("무통장 입금 결제도 complete() 호출 시 COMPLETED 상태가 된다")
		void 무통장입금_complete() {
			Payment payment = createPendingPayment(PaymentMethod.BANK_TRANSFER);
			payment.complete();
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
		}
	}

	@Nested
	@DisplayName("cancel() 결제 취소 처리")
	class Cancel {

		@Test
		@DisplayName("cancel() 호출 시 상태가 CANCELLED로 변경된다")
		void cancel_상태가_CANCELLED() {
			Payment payment = createPendingPayment(PaymentMethod.BANK_TRANSFER);
			payment.cancel();
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
		}

		@Test
		@DisplayName("완료된 결제도 cancel() 호출 시 CANCELLED 상태가 된다")
		void 완료된결제_cancel() {
			Payment payment = createPendingPayment(PaymentMethod.TOSS_PAY);
			payment.complete();
			payment.cancel();
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
		}
	}

	@Nested
	@DisplayName("refund() 환불 처리")
	class Refund {

		@Test
		@DisplayName("refund() 호출 시 상태가 REFUNDED로 변경된다")
		void refund_상태가_REFUNDED() {
			Payment payment = createPendingPayment(PaymentMethod.TOSS_PAY);
			payment.complete();
			payment.refund();
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
		}

		@Test
		@DisplayName("PENDING 상태에서도 refund() 호출 가능하다")
		void refund_PENDING_상태에서_호출가능() {
			Payment payment = createPendingPayment(PaymentMethod.BANK_TRANSFER);
			payment.refund();
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
		}
	}
}
