package com.goormgb.be.domain.onboarding.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.domain.onboarding.entity.OnboardingPreferredBlock;

public interface OnboardingPreferredBlockRepository extends JpaRepository<OnboardingPreferredBlock, Long> {

	List<OnboardingPreferredBlock> findAllByUserId(Long userId);

	long countByUserId(Long userId);

	void deleteAllByUserId(Long userId);
}
