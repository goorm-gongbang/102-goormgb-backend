package com.goormgb.be.auth.dto;

import com.goormgb.be.user.entity.WithdrawalRequest;

import java.time.LocalDateTime;

public record WithdrawlResponse(
        String status,
        LocalDateTime withdrawnAt,
        LocalDateTime reactivateUntil
    ) {
        public static WithdrawlResponse from(WithdrawalRequest request) {
            return new WithdrawlResponse(
                    "DEACTIVE",
                    request.getRequestedAt(),
                    request.getEffectiveAt()
            );
        }
    }
