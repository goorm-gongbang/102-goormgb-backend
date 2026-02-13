package com.goormgb.be.user.fixture;

import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.WithdrawalRequest;

import org.springframework.test.util.ReflectionTestUtils;

public final class WithdrawalRequestFixture {

	private WithdrawalRequestFixture() {
	}

	public static WithdrawalRequest createDefault(User user) {
		return WithdrawalRequest.builder()
				.user(user)
				.build();
	}

	public static WithdrawalRequest createWithId(Long id, User user) {
		WithdrawalRequest request = createDefault(user);
		ReflectionTestUtils.setField(request, "id", id);
		return request;
	}
}
