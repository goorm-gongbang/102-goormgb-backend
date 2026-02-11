package com.goormgb.be.authguard.auth.dto;

import com.goormgb.be.user.entity.WithdrawalRequest;
import com.goormgb.be.user.enums.UserStatus;

import java.time.LocalDateTime;

public record WithdrawalResponse(
		String status,
		LocalDateTime withdrawnAt,
		LocalDateTime reactivateUntil
) {
	public static WithdrawalResponse from(WithdrawalRequest request) {
		return new WithdrawalResponse(
				UserStatus.DEACTIVATE.name(),
				request.getRequestedAt(),
				request.getEffectiveAt()
		);
	}
}
