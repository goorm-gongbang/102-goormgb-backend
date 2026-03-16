package com.goormgb.be.ordercore.cancellation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.ordercore.cancellation.entity.CancellationFeePolicy;

public interface CancellationFeePolicyRepository extends JpaRepository<CancellationFeePolicy, Long> {

	@Query("""
		SELECT c FROM CancellationFeePolicy c
		WHERE c.daysBeforeMatchMin <= :daysLeft
		  AND (c.daysBeforeMatchMax IS NULL OR c.daysBeforeMatchMax >= :daysLeft)
		""")
	Optional<CancellationFeePolicy> findByDaysLeft(@Param("daysLeft") int daysLeft);
}
