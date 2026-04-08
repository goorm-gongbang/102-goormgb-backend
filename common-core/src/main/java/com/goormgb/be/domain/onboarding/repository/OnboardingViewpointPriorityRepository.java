package com.goormgb.be.domain.onboarding.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;

public interface OnboardingViewpointPriorityRepository extends JpaRepository<OnboardingViewpointPriority, Long> {

	List<OnboardingViewpointPriority> findAllByUserIdOrderByPriorityAsc(Long userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM OnboardingViewpointPriority v WHERE v.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Long userId);
}
