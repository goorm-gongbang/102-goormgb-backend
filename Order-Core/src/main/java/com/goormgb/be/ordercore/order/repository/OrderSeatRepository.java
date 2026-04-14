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
