package com.goormgb.be.ordercore.mypage.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.cancellation.entity.CancellationFeePolicy;
import com.goormgb.be.ordercore.cancellation.repository.CancellationFeePolicyRepository;
import com.goormgb.be.ordercore.mypage.dto.query.TicketDetailBaseRow;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketDetailResponse;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;

public final class MyPageTicketDetailAssembler {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private MyPageTicketDetailAssembler() {
	}

	public static MyPageTicketDetailResponse.PaymentInfo toPaymentInfo(TicketDetailBaseRow row) {
		return new MyPageTicketDetailResponse.PaymentInfo(
			row.totalAmount(),
			row.bookingFee(),
			toPaymentMethodValue(row.paymentMethod()),
			row.paidAt(),
			toCashReceipt(row)
		);
	}

	public static MyPageTicketDetailResponse.CancellationPolicy buildCancellationPolicy(
		Instant matchAt,
		Clock clock,
		CancellationFeePolicyRepository cancellationFeePolicyRepository
	) {
		Instant deadline = matchAt.atZone(KST)
			.minusDays(1)
			.withHour(23)
			.withMinute(59)
			.withSecond(0)
			.withNano(0)
			.toInstant();

		int daysLeft = Math.max(0, (int)ChronoUnit.DAYS.between(
			Instant.now(clock).atZone(KST).toLocalDate(),
			matchAt.atZone(KST).toLocalDate()
		));

		CancellationFeePolicy policy = cancellationFeePolicyRepository.findByDaysLeft(daysLeft)
			.orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

		return new MyPageTicketDetailResponse.CancellationPolicy(
			deadline,
			toPercentString(policy.getTicketFeeRate())
		);
	}

	public static MyPageTicketDetailResponse.VirtualAccount toVirtualAccount(TicketDetailBaseRow row) {
		if (row.paymentMethod() != PaymentMethod.BANK_TRANSFER || row.status() != OrderStatus.PAYMENT_PENDING) {
			return null;
		}

		return new MyPageTicketDetailResponse.VirtualAccount(
			row.accountBank(),
			row.accountNumber(),
			row.accountHolder(),
			row.depositDeadline()
		);
	}

	public static MyPageTicketDetailResponse.CancellationInfo toCancellation(TicketDetailBaseRow row) {
		if (row.cancelledAt() == null) {
			return null;
		}

		return new MyPageTicketDetailResponse.CancellationInfo(
			row.cancelledAt(),
			row.cancellationFee(),
			row.refundedAmount()
		);
	}

	private static MyPageTicketDetailResponse.CashReceiptInfo toCashReceipt(TicketDetailBaseRow row) {
		if (row.cashReceiptPurpose() == null || row.cashReceiptNumber() == null) {
			return null;
		}

		return new MyPageTicketDetailResponse.CashReceiptInfo(
			row.cashReceiptPurpose().name(),
			row.cashReceiptNumber(),
			row.totalAmount()
		);
	}

	private static String toPercentString(BigDecimal feeRate) {
		BigDecimal percent = feeRate.multiply(BigDecimal.valueOf(100));
		return percent.setScale(3, RoundingMode.DOWN).stripTrailingZeros().toPlainString() + "%";
	}

	private static String toPaymentMethodValue(PaymentMethod method) {
		if (method == null) {
			return null;
		}
		if (method == PaymentMethod.BANK_TRANSFER) {
			return "VIRTUAL_ACCOUNT";
		}
		return method.name();
	}
}
