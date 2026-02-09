package com.goormgb.be.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.user.entity.DevUser;

public interface DevUserRepository extends JpaRepository<DevUser, Long> {
	Optional<DevUser> findByLoginId(String loginId);

	boolean existsByLoginId(String loginId);
}
