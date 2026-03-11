package com.goormgb.be.seat.pricePolicy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.seat.pricePolicy.entity.PricePolicy;

public interface PricePolicyRepository extends JpaRepository<PricePolicy, Long> {
}
