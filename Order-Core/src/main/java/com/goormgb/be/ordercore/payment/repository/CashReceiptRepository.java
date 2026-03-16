package com.goormgb.be.ordercore.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.ordercore.payment.entity.CashReceipt;

public interface CashReceiptRepository extends JpaRepository<CashReceipt, Long> {

	Optional<CashReceipt> findByPaymentId(Long paymentId);
}
