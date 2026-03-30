package com.goormgb.be.ordercore.payment.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.enums.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByOrderId(Long orderId);

	@Query("""
		SELECT p FROM Payment p
		JOIN FETCH p.order o
		WHERE p.paymentMethod = com.goormgb.be.ordercore.payment.enums.PaymentMethod.BANK_TRANSFER
		  AND p.status = :status
		  AND p.depositDeadline < :now
		""")
	List<Payment> findExpiredBankTransfers(
		@Param("status") PaymentStatus status,
		@Param("now") Instant now
	);
}
