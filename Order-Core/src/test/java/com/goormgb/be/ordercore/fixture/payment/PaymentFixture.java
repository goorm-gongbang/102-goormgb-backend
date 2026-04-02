package com.goormgb.be.ordercore.fixture.payment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.payment.dto.request.CashReceiptCreateRequest;
import com.goormgb.be.ordercore.payment.dto.request.PaymentProcessRequest;
import com.goormgb.be.ordercore.payment.entity.CashReceipt;
import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.enums.CashReceiptPurpose;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;

public final class PaymentFixture {

	private PaymentFixture() {
	}

	public static Payment createBankTransferPayment(Order order) {
		Payment payment = Payment.builder()
				.order(order)
				.paymentMethod(PaymentMethod.BANK_TRANSFER)
				.accountBank("신한은행")
				.accountNumber("110-123-456789")
				.accountHolder("주식회사 구름공방")
				.depositDeadline(Instant.now().plus(3, ChronoUnit.DAYS))
				.build();
		ReflectionTestUtils.setField(payment, "id", 1L);
		return payment;
	}

	public static Payment createCompletedTossPayPayment(Order order) {
		Payment payment = Payment.builder()
				.order(order)
				.paymentMethod(PaymentMethod.TOSS_PAY)
				.build();
		ReflectionTestUtils.setField(payment, "id", 2L);
		payment.complete();
		return payment;
	}

	public static Payment createCompletedKakaoPayPayment(Order order) {
		Payment payment = Payment.builder()
				.order(order)
				.paymentMethod(PaymentMethod.KAKAO_PAY)
				.build();
		ReflectionTestUtils.setField(payment, "id", 3L);
		payment.complete();
		return payment;
	}

	public static CashReceipt createPersonalDeductionReceipt(Payment payment) {
		CashReceipt cashReceipt = CashReceipt.builder()
				.payment(payment)
				.purpose(CashReceiptPurpose.PERSONAL_DEDUCTION)
				.number("010-1234-5678")
				.build();
		ReflectionTestUtils.setField(cashReceipt, "id", 1L);
		return cashReceipt;
	}

	public static CashReceipt createBusinessExpenseReceipt(Payment payment) {
		CashReceipt cashReceipt = CashReceipt.builder()
				.payment(payment)
				.purpose(CashReceiptPurpose.BUSINESS_EXPENSE)
				.number("123-45-67890")
				.build();
		ReflectionTestUtils.setField(cashReceipt, "id", 2L);
		return cashReceipt;
	}

	public static PaymentProcessRequest createBankTransferRequest() {
		return new PaymentProcessRequest(PaymentMethod.BANK_TRANSFER, null, null);
	}

	public static PaymentProcessRequest createTossPayRequest() {
		return new PaymentProcessRequest(PaymentMethod.TOSS_PAY, null, null);
	}

	public static PaymentProcessRequest createKakaoPayRequest() {
		return new PaymentProcessRequest(PaymentMethod.KAKAO_PAY, null, null);
	}

	public static CashReceiptCreateRequest createPersonalDeductionRequest() {
		return new CashReceiptCreateRequest(CashReceiptPurpose.PERSONAL_DEDUCTION, "010-1234-5678");
	}

	public static CashReceiptCreateRequest createBusinessExpenseRequest() {
		return new CashReceiptCreateRequest(CashReceiptPurpose.BUSINESS_EXPENSE, "123-45-67890");
	}
}