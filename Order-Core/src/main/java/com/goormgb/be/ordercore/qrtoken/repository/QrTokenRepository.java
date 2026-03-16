package com.goormgb.be.ordercore.qrtoken.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.ordercore.qrtoken.entity.QrToken;

public interface QrTokenRepository extends JpaRepository<QrToken, Long> {

	Optional<QrToken> findByOrderIdAndExpiresAtAfter(Long orderId, Instant now);
}
