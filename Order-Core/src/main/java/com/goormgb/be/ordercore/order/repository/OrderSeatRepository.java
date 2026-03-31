package com.goormgb.be.ordercore.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.ordercore.order.entity.OrderSeat;

public interface OrderSeatRepository extends JpaRepository<OrderSeat, Long> {

	List<OrderSeat> findByOrderId(Long orderId);

	@Query("SELECT os.matchSeatId FROM OrderSeat os WHERE os.order.id = :orderId")
	List<Long> findMatchSeatIdsByOrderId(@Param("orderId") Long orderId);

	@Modifying
	@Query("DELETE FROM OrderSeat os WHERE os.order.id IN :orderIds")
	int deleteByOrderIdIn(@Param("orderIds") List<Long> orderIds);
}
