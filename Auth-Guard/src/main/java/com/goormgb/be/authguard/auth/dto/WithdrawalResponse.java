package com.goormgb.be.authguard.auth.dto;

import java.time.Instant;

import com.goormgb.be.user.entity.WithdrawalRequest;
import com.goormgb.be.user.enums.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 탈퇴 응답")
public record WithdrawalResponse(
		@Schema(description = "계정 상태", example = "DEACTIVATE")
		String status,
		@Schema(description = "탈퇴 신청 시각 (UTC ISO-8601)", type = "string", example = "2026-03-10T10:00:00Z")
		Instant withdrawnAt,
		@Schema(description = "계정 최종 삭제 예정 시각 - 유예 기간 30일 (UTC ISO-8601)", type = "string", example = "2026-04-09T10:00:00Z")
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
