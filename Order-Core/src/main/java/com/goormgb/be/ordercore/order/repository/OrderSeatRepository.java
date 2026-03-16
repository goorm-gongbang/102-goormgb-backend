package com.goormgb.be.ordercore.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.ordercore.order.entity.OrderSeat;

public interface OrderSeatRepository extends JpaRepository<OrderSeat, Long> {

	List<OrderSeat> findByOrderId(Long orderId);
}
