package com.goormgb.be.ordercore.order.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

	long countByUserId(Long userId);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status IN :statuses AND o.match.matchAt > :now")
	long countUpcomingOrders(@Param("userId") Long userId,
		@Param("statuses") List<OrderStatus> statuses,
		@Param("now") Instant now);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status = 'PAID' AND o.match.matchAt < :now")
	long countCompletedOrders(@Param("userId") Long userId, @Param("now") Instant now);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status IN :statuses")
	long countByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<OrderStatus> statuses);
}
