package com.goormgb.be.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.user.dto.response.UserInfoGetResponse;
import com.goormgb.be.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
	private final UserRepository userRepository;

	public UserInfoGetResponse getMyInfo(Long id) {
		var user = userRepository.findByIdOrThrow(id, ErrorCode.USER_NOT_FOUND);

		return UserInfoGetResponse.from(user);
	}
}
