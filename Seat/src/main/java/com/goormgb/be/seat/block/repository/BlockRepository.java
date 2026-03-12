package com.goormgb.be.seat.block.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.goormgb.be.seat.block.entity.Block;

public interface BlockRepository extends JpaRepository<Block, Long> {

	@Query("SELECT b FROM Block b JOIN FETCH b.section s JOIN FETCH b.area a ORDER BY b.blockCode ASC")
	List<Block> findAllWithSectionAndArea();
}
