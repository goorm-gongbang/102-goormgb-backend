package com.goormgb.be.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.user.entity.LoadTestUser;

public interface LoadTestUserRepository extends JpaRepository<LoadTestUser, Long> {
	Optional<LoadTestUser> findByLoginId(String loginId);

	boolean existsByLoginId(String loginId);

	long countByLoginIdStartingWith(String prefix);
}
