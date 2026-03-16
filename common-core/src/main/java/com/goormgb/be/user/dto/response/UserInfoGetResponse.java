package com.goormgb.be.user.dto.response;

import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.enums.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 내 정보 조회 응답")
public record UserInfoGetResponse(
	@Schema(description = "유저 ID", example = "1")
	Long id,
	@Schema(description = "계정 상태", example = "ACTIVATE")
	UserStatus status,
	@Schema(description = "이메일", example = "user@example.com")
	String email,
	@Schema(description = "닉네임", example = "홍길동")
	String nickname
) {
	public static UserInfoGetResponse from(User user) {
		return new UserInfoGetResponse(
			user.getId(),
			user.getStatus(),
			user.getEmail(),
			user.getNickname()
		);
	}
}
