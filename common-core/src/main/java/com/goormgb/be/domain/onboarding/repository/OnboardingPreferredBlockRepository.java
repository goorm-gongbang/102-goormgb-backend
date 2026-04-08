package com.goormgb.be.domain.onboarding.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.domain.onboarding.entity.OnboardingPreferredBlock;

public interface OnboardingPreferredBlockRepository extends JpaRepository<OnboardingPreferredBlock, Long> {

	List<OnboardingPreferredBlock> findAllByUserId(Long userId);

	@Query("SELECT opb.blockId FROM OnboardingPreferredBlock opb WHERE opb.user.id = :userId")
	List<Long> findBlockIdsByUserId(@Param("userId") Long userId);

	long countByUserId(Long userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM OnboardingPreferredBlock opb WHERE opb.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Long userId);
}
