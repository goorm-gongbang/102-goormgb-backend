package com.goormgb.be.ordercore.payment.dto.request;

import com.goormgb.be.ordercore.payment.enums.CashReceiptPurpose;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentProcessRequest(

	@NotNull(message = "결제 수단은 필수입니다.")
	PaymentMethod paymentMethod,

	CashReceiptPurpose cashReceiptPurpose,

	@Size(max = 50)
	String cashReceiptNumber
) {

	public boolean hasCashReceipt() {
		return cashReceiptPurpose != null && cashReceiptNumber != null && !cashReceiptNumber.isBlank();
	}
}

