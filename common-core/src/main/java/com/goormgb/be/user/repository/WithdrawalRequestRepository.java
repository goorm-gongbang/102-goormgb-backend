package com.goormgb.be.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.user.entity.WithdrawalRequest;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
}
