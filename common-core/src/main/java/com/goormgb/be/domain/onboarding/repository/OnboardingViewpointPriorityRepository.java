package com.goormgb.be.domain.onboarding.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;

public interface OnboardingViewpointPriorityRepository extends JpaRepository<OnboardingViewpointPriority, Long> {

	List<OnboardingViewpointPriority> findAllByUserIdOrderByPriorityAsc(Long userId);

	void deleteAllByUserId(Long userId);
}
