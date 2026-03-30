package com.goormgb.be.ordercore.mypage.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.cancellation.entity.CancellationFeePolicy;
import com.goormgb.be.ordercore.cancellation.repository.CancellationFeePolicyRepository;
import com.goormgb.be.ordercore.order.entity.Order;

public final class MyPageTicketCancellationCalculator {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private MyPageTicketCancellationCalculator() {
	}

	public static CancellationFeePolicy findCancellationPolicy(
		Instant matchAt,
		Instant now,
		CancellationFeePolicyRepository cancellationFeePolicyRepository
	) {
		int daysLeft = Math.max(0, (int)ChronoUnit.DAYS.between(
			now.atZone(KST).toLocalDate(),
			matchAt.atZone(KST).toLocalDate()
		));
		return cancellationFeePolicyRepository.findByDaysLeft(daysLeft)
			.orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	public static int calculateCancellationFee(Order order, CancellationFeePolicy policy, Instant now) {
		int ticketAmount = Math.max(0, order.getTotalAmount() - order.getBookingFee());
		int ticketFee = policy.getTicketFeeRate()
			.multiply(BigDecimal.valueOf(ticketAmount))
			.setScale(0, RoundingMode.DOWN)
			.intValue();

		if (Boolean.TRUE.equals(policy.getBookingFeeRefundable()) && isSameBookingDate(order, now)) {
			return ticketFee;
		}
		return ticketFee + order.getBookingFee();
	}

	private static boolean isSameBookingDate(Order order, Instant now) {
		if (order.getCreatedAt() == null) {
			return false;
		}
		LocalDate bookingDate = order.getCreatedAt().atZone(KST).toLocalDate();
		LocalDate cancelDate = now.atZone(KST).toLocalDate();
		return bookingDate.equals(cancelDate);
	}
}
