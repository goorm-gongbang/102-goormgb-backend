package com.goormgb.be.ordercore.mypage.dto.response;

import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.UserSns;

public record MyPageAccountResponse(
	String email,
	String nickname,
	SnsAccount snsAccount
) {

	public record SnsAccount(
		String provider,
		String providerUserId
	) {
	}

	public static MyPageAccountResponse of(User user, UserSns userSns) {
		SnsAccount snsAccount = userSns == null
			? null
			: new SnsAccount(userSns.getProvider().name(), userSns.getProviderUserId());
		return new MyPageAccountResponse(
			user.getEmail(),
			user.getNickname(),
			snsAccount
		);
	}
}
