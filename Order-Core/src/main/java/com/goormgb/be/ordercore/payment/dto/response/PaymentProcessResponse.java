package com.goormgb.be.ordercore.payment.dto.response;

import java.time.Instant;

import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;
import com.goormgb.be.ordercore.payment.enums.PaymentStatus;

public record PaymentProcessResponse(
	Long orderId,
	OrderStatus orderStatus,
	PaymentMethod paymentMethod,
	PaymentStatus paymentStatus,
	Instant paidAt,
	VirtualAccountInfo virtualAccount
) {

	public record VirtualAccountInfo(
		String bank,
		String accountNumber,
		String holder,
		Instant depositDeadline
	) {}

	public static PaymentProcessResponse of(Payment payment) {
		VirtualAccountInfo vaInfo = null;
		if (payment.getVirtualAccountBank() != null) {
			vaInfo = new VirtualAccountInfo(
				payment.getVirtualAccountBank(),
				payment.getVirtualAccountNumber(),
				payment.getVirtualAccountHolder(),
				payment.getDepositDeadline()
			);
		}

		return new PaymentProcessResponse(
			payment.getOrder().getId(),
			payment.getOrder().getStatus(),
			payment.getPaymentMethod(),
			payment.getStatus(),
			payment.getPaidAt(),
			vaInfo
		);
	}
}
