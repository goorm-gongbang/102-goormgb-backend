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
		@DisplayName("간편결제 생성 시 가상계좌 정보는 null이다")
		void 간편결제_생성시_가상계좌_null() {
			Payment payment = createPendingPayment(PaymentMethod.KAKAO_PAY);
			assertThat(payment.getVirtualAccountBank()).isNull();
			assertThat(payment.getVirtualAccountNumber()).isNull();
			assertThat(payment.getVirtualAccountHolder()).isNull();
			assertThat(payment.getDepositDeadline()).isNull();
		}

		@Test
		@DisplayName("가상계좌 생성 시 계좌 정보가 올바르게 설정된다")
		void 가상계좌_생성시_계좌정보_설정() {
			Instant deadline = Instant.now().plus(3, ChronoUnit.DAYS);
			Payment payment = Payment.builder()
				.order(createOrder())
				.paymentMethod(PaymentMethod.VIRTUAL_ACCOUNT)
				.virtualAccountBank("국민은행")
				.virtualAccountNumber("047-000-00000001")
				.virtualAccountHolder("구름GB")
				.depositDeadline(deadline)
				.build();

			assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.VIRTUAL_ACCOUNT);
			assertThat(payment.getVirtualAccountBank()).isEqualTo("국민은행");
			assertThat(payment.getVirtualAccountNumber()).isEqualTo("047-000-00000001");
			assertThat(payment.getVirtualAccountHolder()).isEqualTo("구름GB");
			assertThat(payment.getDepositDeadline()).isEqualTo(deadline);
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
		}

		@Test
		@DisplayName("결제 수단이 올바르게 저장된다")
		void 결제수단_저장() {
			Payment tossPay = createPendingPayment(PaymentMethod.TOSS_PAY);
			Payment kakaoPay = createPendingPayment(PaymentMethod.KAKAO_PAY);
			Payment virtualAccount = createPendingPayment(PaymentMethod.VIRTUAL_ACCOUNT);

			assertThat(tossPay.getPaymentMethod()).isEqualTo(PaymentMethod.TOSS_PAY);
			assertThat(kakaoPay.getPaymentMethod()).isEqualTo(PaymentMethod.KAKAO_PAY);
			assertThat(virtualAccount.getPaymentMethod()).isEqualTo(PaymentMethod.VIRTUAL_ACCOUNT);
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
		@DisplayName("가상계좌 결제도 complete() 호출 시 COMPLETED 상태가 된다")
		void 가상계좌_complete() {
			Payment payment = createPendingPayment(PaymentMethod.VIRTUAL_ACCOUNT);
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
			Payment payment = createPendingPayment(PaymentMethod.VIRTUAL_ACCOUNT);
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
			Payment payment = createPendingPayment(PaymentMethod.VIRTUAL_ACCOUNT);
			payment.refund();
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
		}
	}
}
