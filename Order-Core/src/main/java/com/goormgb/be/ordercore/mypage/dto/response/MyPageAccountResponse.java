package com.goormgb.be.ordercore.mypage.dto.response;

import java.util.List;

import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.UserSns;

public record MyPageAccountResponse(
	String email,
	String nickname,
	List<SnsAccount> snsAccounts
) {

	public record SnsAccount(
		String provider,
		String providerUserId
	) {
	}

	public static MyPageAccountResponse of(User user, List<UserSns> userSnsList) {
		List<SnsAccount> snsAccounts = userSnsList.stream()
			.map(sns -> new SnsAccount(sns.getProvider().name(), sns.getProviderUserId()))
			.toList();
		return new MyPageAccountResponse(
			user.getEmail(),
			user.getNickname(),
			snsAccounts
		);
	}
}
