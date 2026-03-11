package com.goormgb.be.seat.block.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.seat.block.entity.Block;

public interface BlockRepository extends JpaRepository<Block, Long> {
}
