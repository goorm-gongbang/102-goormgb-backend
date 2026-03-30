package com.goormgb.be.ordercore.mypage.service.support;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.qrtoken.entity.QrToken;
import com.goormgb.be.ordercore.qrtoken.repository.QrTokenRepository;

public final class MyPageTicketQrSupport {

	private static final long QR_REFRESH_INTERVAL_SECONDS = 180L;
	private static final Duration ENTRY_OPEN_BEFORE_MATCH = Duration.ofHours(3);

	private MyPageTicketQrSupport() {
	}

	public static void validateQrIssuableTime(Instant matchAt, Instant now) {
		Preconditions.validate(now.isBefore(matchAt), ErrorCode.ENTRY_QR_MATCH_STARTED);
		Preconditions.validate(!now.isBefore(matchAt.minus(ENTRY_OPEN_BEFORE_MATCH)),
			ErrorCode.ENTRY_QR_NOT_AVAILABLE_YET);
	}

	public static QrToken issueNewQrToken(Order order, Instant now, QrTokenRepository qrTokenRepository) {
		QrToken qrToken = QrToken.builder()
			.order(order)
			.user(order.getUser())
			.qrToken(UUID.randomUUID().toString())
			.expiresAt(calculateNextQrExpiry(now))
			.build();
		return qrTokenRepository.save(qrToken);
	}

	private static Instant calculateNextQrExpiry(Instant now) {
		long nowEpochSec = now.getEpochSecond();
		long expiresEpochSec = ((nowEpochSec / QR_REFRESH_INTERVAL_SECONDS) + 1) * QR_REFRESH_INTERVAL_SECONDS;
		return Instant.ofEpochSecond(expiresEpochSec);
	}
}
