package com.goormgb.be.ordercore.payment.dto.request;

import com.goormgb.be.ordercore.payment.enums.CashReceiptPurpose;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CashReceiptCreateRequest(

	@NotNull(message = "현금영수증 용도는 필수입니다.")
	CashReceiptPurpose purpose,

	@NotBlank(message = "현금영수증 번호는 필수입니다.")
	@Size(max = 50)
	String number
) {
}
