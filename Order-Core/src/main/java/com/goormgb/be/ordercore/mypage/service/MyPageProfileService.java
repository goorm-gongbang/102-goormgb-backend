package com.goormgb.be.ordercore.mypage.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.mypage.dto.request.MyPageAccountUpdateRequest;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageAccountResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.repository.OrderMyPageSummaryCounts;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.UserSns;
import com.goormgb.be.user.repository.UserRepository;
import com.goormgb.be.user.repository.UserSnsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageProfileService {

	private static final List<OrderStatus> UPCOMING_STATUSES = List.of(
		OrderStatus.PAYMENT_PENDING,
		OrderStatus.PAID
	);

	private static final List<OrderStatus> CANCEL_REFUND_STATUSES = List.of(
		OrderStatus.CANCEL_REQUESTED,
		OrderStatus.CANCELLED,
		OrderStatus.REFUND_PROCESSING,
		OrderStatus.REFUND_COMPLETED
	);

	private final UserRepository userRepository;
	private final UserSnsRepository userSnsRepository;
	private final OrderRepository orderRepository;
	private final Clock clock;

	private record UserInfo(User user, UserSns userSns) {
	}

	private UserInfo findUserInfo(Long userId) {
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);
		UserSns userSns = userSnsRepository.findByUserId(userId).orElse(null);
		return new UserInfo(user, userSns);
	}

	@Transactional
	public MyPageAccountResponse updateAccount(Long userId, MyPageAccountUpdateRequest request) {
		String nickname = request.nickname().trim();
		UserInfo userInfo = findUserInfo(userId);
		userInfo.user().updateNickname(nickname);
		return MyPageAccountResponse.of(userInfo.user(), userInfo.userSns());
	}

	public MyPageAccountResponse getAccount(Long userId) {
		UserInfo userInfo = findUserInfo(userId);
		return MyPageAccountResponse.of(userInfo.user(), userInfo.userSns());
	}

	public MyPageProfileResponse getProfile(Long userId) {
		UserInfo userInfo = findUserInfo(userId);

		Instant now = Instant.now(clock);
		OrderMyPageSummaryCounts counts = orderRepository.findMyPageSummaryCounts(
			userId,
			UPCOMING_STATUSES,
			CANCEL_REFUND_STATUSES,
			CANCEL_REFUND_STATUSES,
			OrderStatus.PAID,
			now
		);
		long upcomingCount = counts.upcomingCount();
		long cancelRefundCount = counts.cancelRefundCount();
		long completedCount = counts.completedCount();

		log.info("[MyPageProfileService] 프로필 조회 - userId={}, upcomingCount={}, cancelRefundCount={}, completedCount={}",
			userId, upcomingCount, cancelRefundCount, completedCount);

		return MyPageProfileResponse.of(
			userInfo.user(),
			userInfo.userSns(),
			upcomingCount,
			cancelRefundCount,
			completedCount
		);
	}

}
