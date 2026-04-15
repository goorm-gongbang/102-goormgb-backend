package com.goormgb.be.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	@Query(value = """
		SELECT EXISTS (
			SELECT 1
			FROM orders o
			JOIN matches m ON m.id = o.match_id
			WHERE o.user_id = :userId
			  AND o.status = 'PAID'
			  AND m.match_at > CURRENT_TIMESTAMP
		)
		""", nativeQuery = true)
	boolean existsValidPaidTicketForWithdrawal(@Param("userId") Long userId);

	@Query(value = """
		SELECT EXISTS (
			SELECT 1
			FROM orders o
			WHERE o.user_id = :userId
			  AND o.status = 'REFUND_PROCESSING'
		)
		""", nativeQuery = true)
	boolean existsRefundProcessingOrder(@Param("userId") Long userId);

	@Query(value = """
		SELECT EXISTS (
			SELECT 1
			FROM orders o
			WHERE o.user_id = :userId
			  AND o.status IN ('PAYMENT_PENDING', 'UNDER_REVIEW')
		)
		""", nativeQuery = true)
	boolean existsOngoingTransactionOrSettlement(@Param("userId") Long userId);

	default User findByIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id).orElseThrow(() -> new CustomException(errorCode));
	}
}
