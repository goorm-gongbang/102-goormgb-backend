package com.goormgb.be.domain.onboarding.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.domain.onboarding.entity.OnboardingPreferredBlock;

public interface OnboardingPreferredBlockRepository extends JpaRepository<OnboardingPreferredBlock, Long> {

	List<OnboardingPreferredBlock> findAllByUserId(Long userId);

	@Query("SELECT opb.blockId FROM OnboardingPreferredBlock opb WHERE opb.user.id = :userId")
	List<Long> findBlockIdsByUserId(@Param("userId") Long userId);

	long countByUserId(Long userId);

	void deleteAllByUserId(Long userId);
}
