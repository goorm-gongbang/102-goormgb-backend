package com.goormgb.be.domain.onboarding.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;

public interface OnboardingPreferenceRepository extends JpaRepository<OnboardingPreference, Long> {

	Optional<OnboardingPreference> findByUserId(Long userId);

	boolean existsByUserId(Long userId);

	void deleteByUserId(Long userId);

	default OnboardingPreference findByUserIdOrThrow(Long userId, ErrorCode errorCode) {
		return findByUserId(userId).orElseThrow(() -> new CustomException(errorCode));
	}

	default OnboardingPreference findByIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id).orElseThrow(() -> new CustomException(errorCode));
	}
}
