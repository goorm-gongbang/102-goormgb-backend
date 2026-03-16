package com.goormgb.be.ordercore.payment.dto.request;

import com.goormgb.be.ordercore.payment.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record PaymentProcessRequest(

	@NotNull(message = "결제 수단은 필수입니다.")
	PaymentMethod paymentMethod
) {
}
