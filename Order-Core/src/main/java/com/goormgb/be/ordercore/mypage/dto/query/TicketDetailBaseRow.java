package com.goormgb.be.ordercore.mypage.dto.query;

import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.payment.enums.CashReceiptPurpose;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;

public record TicketDetailBaseRow(
	Long orderId,
	OrderStatus status,
	int totalAmount,
	int bookingFee,
	java.time.Instant cancelledAt,
	int cancellationFee,
	Integer refundedAmount,
	Long matchId,
	java.time.Instant matchAt,
	Long homeClubId,
	String homeClubName,
	Long awayClubId,
	String awayClubName,
	Long stadiumId,
	String stadiumName,
	String stadiumAddress,
	PaymentMethod paymentMethod,
	java.time.Instant paidAt,
	String accountBank,
	String accountNumber,
	String accountHolder,
	java.time.Instant depositDeadline,
	CashReceiptPurpose cashReceiptPurpose,
	String cashReceiptNumber
) {
}
