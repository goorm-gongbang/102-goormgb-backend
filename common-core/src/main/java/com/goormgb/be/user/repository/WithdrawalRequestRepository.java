package com.goormgb.be.user.repository;

import com.goormgb.be.user.entity.WithdrawalRequest;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
}
