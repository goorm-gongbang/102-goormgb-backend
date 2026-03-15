package com.goormgb.be.ordercore.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.ordercore.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
