package com.goormgb.be.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.user.entity.LoadTestUser;

public interface LoadTestUserRepository extends JpaRepository<LoadTestUser, Long> {
	Optional<LoadTestUser> findByLoginId(String loginId);

	boolean existsByLoginId(String loginId);

	@Query("SELECT lt.loginId FROM LoadTestUser lt WHERE lt.loginId IN :loginIds")
	Set<String> findExistingLoginIds(@Param("loginIds") List<String> loginIds);
}
