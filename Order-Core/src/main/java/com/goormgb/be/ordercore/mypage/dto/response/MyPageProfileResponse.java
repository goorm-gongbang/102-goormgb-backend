package com.goormgb.be.ordercore.mypage.dto.response;

import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.UserSns;

public record MyPageProfileResponse(
	ProfileInfo profile,
	TicketSummary ticketSummary
) {

	public record ProfileInfo(
		String nickname,
		String profileImageUrl,
		String snsProvider
	) {
	}

	public record TicketSummary(
		int upcomingCount,
		int cancelRefundCount,
		int completedCount
	) {
	}

	public static MyPageProfileResponse of(
		User user,
		UserSns userSns,
		long upcomingCount,
		long cancelRefundCount,
		long completedCount
	) {
		String snsProvider = userSns != null ? userSns.getProvider().name() : null;
		return new MyPageProfileResponse(
			new ProfileInfo(user.getNickname(), user.getProfileImageUrl(), snsProvider),
			new TicketSummary((int)upcomingCount, (int)cancelRefundCount, (int)completedCount)
		);
	}
}
