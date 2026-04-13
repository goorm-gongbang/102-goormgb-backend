package com.goormgb.be.ordercore.order.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.ordercore.order.entity.OrderSeat;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.payment.enums.PaymentStatus;

public interface OrderSeatRepository extends JpaRepository<OrderSeat, Long> {

	/**
	 * 특정 유저의 특정 경기에 대한 유효 주문 좌석 수를 카운트한다.
	 */
	@Query("""
			SELECT COUNT(os)
			FROM OrderSeat os
			JOIN os.order o
			WHERE o.user.id = :userId
			  AND o.match.id = :matchId
			  AND o.status IN :statuses
			""")
	long countByUserIdAndMatchIdAndStatuses(
			@Param("userId") Long userId,
			@Param("matchId") Long matchId,
			@Param("statuses") List<OrderStatus> statuses
	);

	List<OrderSeat> findByOrderId(Long orderId);

	@Query("SELECT os.matchSeatId FROM OrderSeat os WHERE os.order.id = :orderId")
	List<Long> findMatchSeatIdsByOrderId(@Param("orderId") Long orderId);

	@Modifying
	@Query("DELETE FROM OrderSeat os WHERE os.order.id IN :orderIds")
	int deleteByOrderIdIn(@Param("orderIds") List<Long> orderIds);

	@Modifying
	@Query("DELETE FROM OrderSeat os WHERE os.order.id = :orderId")
	int deleteByOrderId(@Param("orderId") Long orderId);

	@Modifying
	@Query("""
		DELETE FROM OrderSeat os
		WHERE os.matchSeatId IN :matchSeatIds
		  AND os.order.status IN :statuses
		""")
	int deleteByMatchSeatIdInAndOrderStatuses(
		@Param("matchSeatIds") List<Long> matchSeatIds,
		@Param("statuses") List<OrderStatus> statuses
	);

	@Modifying
	@Query("""
		DELETE FROM OrderSeat os
		WHERE os.matchSeatId IN :matchSeatIds
		  AND os.order.status = :pendingOrderStatus
		  AND EXISTS (
		  	SELECT 1
		  	FROM Payment p
		  	WHERE p.order = os.order
		  	  AND p.status = :pendingPaymentStatus
		  	  AND p.depositDeadline IS NOT NULL
		  	  AND p.depositDeadline < :now
		  )
		""")
	int deleteExpiredPendingBankTransferSeats(
		@Param("matchSeatIds") List<Long> matchSeatIds,
		@Param("pendingOrderStatus") OrderStatus pendingOrderStatus,
		@Param("pendingPaymentStatus") PaymentStatus pendingPaymentStatus,
		@Param("now") Instant now
	);
}
