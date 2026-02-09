package com.goormgb.be.user.service;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.user.dto.response.OnboardingStatusResponse;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public OnboardingStatusResponse getOnboardingStatus(Long userId) {
        User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);

        return OnboardingStatusResponse.from(user);
    }
}
