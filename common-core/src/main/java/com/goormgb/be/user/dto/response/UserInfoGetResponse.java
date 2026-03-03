package com.goormgb.be.user.dto.response;

import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.enums.UserStatus;

public record UserInfoGetResponse(
	Long id,
	UserStatus status,
	String email,
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
