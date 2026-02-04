package com.goormgb.be.onboarding.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.onboarding.entity.OnboardingPreference;
import com.goormgb.be.onboarding.enums.Viewpoint;

public interface OnboardingPreferenceRepository extends JpaRepository<OnboardingPreference, Long> {
	List<OnboardingPreference> findAllByUserId(Long userId);

	List<OnboardingPreference> findAllByUserIdOrderByPriorityAsc(Long userId);

	Optional<OnboardingPreference> findByUserIdAndPriority(Long userId, Integer priority);

	Optional<OnboardingPreference> findByUserIdAndViewpoint(Long userId, Viewpoint viewpoint);

	boolean existsByUserIdAndPriority(Long userId, Integer priority);

	boolean existsByUserIdAndViewpoint(Long userId, Viewpoint viewpoint);

	void deleteAllByUserId(Long userId);

	default OnboardingPreference findByIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id).orElseThrow(() -> new CustomException(errorCode));
	}
}
