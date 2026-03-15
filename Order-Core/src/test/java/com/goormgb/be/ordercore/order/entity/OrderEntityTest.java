package com.goormgb.be.ordercore.order.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.goormgb.be.ordercore.fixture.order.OrderFixture;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.user.entity.User;

@DisplayName("Order 엔티티 단위 테스트")
class OrderEntityTest {

	private Order createTestOrder() {
		User user = OrderFixture.createUser();
		return Order.builder()
			.user(user)
			.match(OrderFixture.createWeekdayMatch())
			.totalAmount(24000)
			.ordererName("홍길동")
			.ordererEmail("hong@test.com")
			.ordererPhone("010-1234-5678")
			.ordererBirthDate("990831")
			.build();
	}

	@Nested
	@DisplayName("생성 시 초기 상태 검증")
	class Creation {

		@Test
		@DisplayName("주문 생성 시 상태는 PAYMENT_PENDING이다")
		void 생성시_상태는_PAYMENT_PENDING() {
			Order order = createTestOrder();
			assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
		}

		@Test
		@DisplayName("주문 생성 시 예약 수수료는 2000원이다")
		void 생성시_예약수수료는_2000() {
			Order order = createTestOrder();
			assertThat(order.getBookingFee()).isEqualTo(2000);
		}

		@Test
		@DisplayName("주문 생성 시 취소 수수료는 0원이다")
		void 생성시_취소수수료는_0() {
			Order order = createTestOrder();
			assertThat(order.getCancellationFee()).isEqualTo(0);
		}

		@Test
		@DisplayName("주문 생성 시 환불 금액은 null이다")
		void 생성시_환불금액은_null() {
			Order order = createTestOrder();
			assertThat(order.getRefundedAmount()).isNull();
		}

		@Test
		@DisplayName("주문 생성 시 취소 시각은 null이다")
		void 생성시_취소시각은_null() {
			Order order = createTestOrder();
			assertThat(order.getCancelledAt()).isNull();
		}

		@Test
		@DisplayName("주문 생성 시 예매자 정보가 올바르게 저장된다")
		void 생성시_예매자정보_저장() {
			Order order = createTestOrder();
			assertThat(order.getOrdererName()).isEqualTo("홍길동");
			assertThat(order.getOrdererEmail()).isEqualTo("hong@test.com");
			assertThat(order.getOrdererPhone()).isEqualTo("010-1234-5678");
			assertThat(order.getOrdererBirthDate()).isEqualTo("990831");
		}

		@Test
		@DisplayName("주문 생성 시 총 금액이 올바르게 저장된다")
		void 생성시_총금액_저장() {
			Order order = createTestOrder();
			assertThat(order.getTotalAmount()).isEqualTo(24000);
		}
	}

	@Nested
	@DisplayName("updateStatus() 상태 변경")
	class UpdateStatus {

		@Test
		@DisplayName("PAYMENT_PENDING → PAID로 상태를 변경할 수 있다")
		void updateStatus_PAID로_변경() {
			Order order = createTestOrder();
			order.updateStatus(OrderStatus.PAID);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		}

		@Test
		@DisplayName("PAYMENT_PENDING → CANCEL_REQUESTED로 상태를 변경할 수 있다")
		void updateStatus_CANCEL_REQUESTED로_변경() {
			Order order = createTestOrder();
			order.updateStatus(OrderStatus.CANCEL_REQUESTED);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);
		}

		@Test
		@DisplayName("PAID → REFUND_PROCESSING으로 상태를 변경할 수 있다")
		void updateStatus_REFUND_PROCESSING으로_변경() {
			Order order = createTestOrder();
			order.updateStatus(OrderStatus.PAID);
			order.updateStatus(OrderStatus.REFUND_PROCESSING);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_PROCESSING);
		}

		@Test
		@DisplayName("REFUND_PROCESSING → REFUND_COMPLETED로 상태를 변경할 수 있다")
		void updateStatus_REFUND_COMPLETED로_변경() {
			Order order = createTestOrder();
			order.updateStatus(OrderStatus.REFUND_PROCESSING);
			order.updateStatus(OrderStatus.REFUND_COMPLETED);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_COMPLETED);
		}
	}

	@Nested
	@DisplayName("cancel() 취소 처리")
	class Cancel {

		@Test
		@DisplayName("cancel() 호출 시 상태가 CANCEL_REQUESTED로 변경된다")
		void cancel_상태가_CANCEL_REQUESTED() {
			Order order = createTestOrder();
			order.cancel(1000, 23000);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);
		}

		@Test
		@DisplayName("cancel() 호출 시 취소 수수료가 저장된다")
		void cancel_취소수수료_저장() {
			Order order = createTestOrder();
			order.cancel(1000, 23000);
			assertThat(order.getCancellationFee()).isEqualTo(1000);
		}

		@Test
		@DisplayName("cancel() 호출 시 환불 금액이 저장된다")
		void cancel_환불금액_저장() {
			Order order = createTestOrder();
			order.cancel(1000, 23000);
			assertThat(order.getRefundedAmount()).isEqualTo(23000);
		}

		@Test
		@DisplayName("cancel() 호출 시 취소 시각이 설정된다")
		void cancel_취소시각_설정() {
			Order order = createTestOrder();
			Instant before = Instant.now();
			order.cancel(0, 24000);
			assertThat(order.getCancelledAt()).isNotNull();
			assertThat(order.getCancelledAt()).isAfterOrEqualTo(before);
		}

		@Test
		@DisplayName("취소 수수료 0원, 전액 환불 시에도 정상 처리된다")
		void cancel_전액환불_정상처리() {
			Order order = createTestOrder();
			order.cancel(0, 24000);
			assertThat(order.getCancellationFee()).isEqualTo(0);
			assertThat(order.getRefundedAmount()).isEqualTo(24000);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);
		}
	}
}
