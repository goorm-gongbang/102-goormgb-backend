package com.goormgb.be.authguard.auth.dto;

import java.time.Instant;

import com.goormgb.be.user.entity.WithdrawalRequest;
import com.goormgb.be.user.enums.UserStatus;

public record WithdrawalResponse(
		String status,
		Instant withdrawnAt,
		Instant reactivateUntil
) {
	public static WithdrawalResponse from(WithdrawalRequest request) {
		return new WithdrawalResponse(
				UserStatus.DEACTIVATE.name(),
				request.getRequestedAt(),
				request.getEffectiveAt()
		);
	}
}
