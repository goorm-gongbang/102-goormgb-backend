package com.goormgb.be.ordercore.payment.dto.response;

import com.goormgb.be.ordercore.payment.entity.CashReceipt;
import com.goormgb.be.ordercore.payment.enums.CashReceiptPurpose;

public record CashReceiptCreateResponse(
	Long orderId,
	CashReceiptPurpose purpose,
	String number
) {

	public static CashReceiptCreateResponse of(Long orderId, CashReceipt cashReceipt) {
		return new CashReceiptCreateResponse(
			orderId,
			cashReceipt.getPurpose(),
			cashReceipt.getNumber()
		);
	}
}
