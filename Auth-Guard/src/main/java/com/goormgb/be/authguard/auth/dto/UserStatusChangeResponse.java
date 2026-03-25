package com.goormgb.be.authguard.auth.dto;

import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.enums.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 상태 변경 응답")
public record UserStatusChangeResponse(
		@Schema(description = "상태가 변경된 대상 유저 ID", example = "7")
		Long userId,
		@Schema(description = "변경 후 유저 상태", example = "BLOCKED")
		UserStatus status
) {
	public static UserStatusChangeResponse from(User user) {
		return new UserStatusChangeResponse(
				user.getId(),
				user.getStatus()
		);
	}
}
