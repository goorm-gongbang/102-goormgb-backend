package com.goormgb.be.ordercore.payment.dto.response;

import java.time.Instant;

import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;
import com.goormgb.be.ordercore.payment.enums.PaymentStatus;

public record PaymentProcessResponse(
	Long orderId,
	String orderNumber,
	OrderStatus orderStatus,
	PaymentMethod paymentMethod,
	PaymentStatus paymentStatus,
	Instant paidAt,
	AccountInfo account
) {

	public record AccountInfo(
		String bank,
		String accountNumber,
		String holder,
		Instant depositDeadline
	) {}

	public static PaymentProcessResponse of(Payment payment) {
		AccountInfo accountInfo = null;
		if (payment.getAccountBank() != null) {
			accountInfo = new AccountInfo(
				payment.getAccountBank(),
				payment.getAccountNumber(),
				payment.getAccountHolder(),
				payment.getDepositDeadline()
			);
		}

		return new PaymentProcessResponse(
			payment.getOrder().getId(),
			payment.getOrder().getOrderNumber(),
			payment.getOrder().getStatus(),
			payment.getPaymentMethod(),
			payment.getStatus(),
			payment.getPaidAt(),
			accountInfo
		);
	}
}
