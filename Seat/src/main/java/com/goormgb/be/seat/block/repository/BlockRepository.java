package com.goormgb.be.seat.block.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.block.entity.Block;

public interface BlockRepository extends JpaRepository<Block, Long> {

	@Query("SELECT b FROM Block b JOIN FETCH b.section s JOIN FETCH b.area a ORDER BY b.blockCode ASC")
	List<Block> findAllWithSectionAndArea();

	List<Block> findBySectionIdInOrderBySectionIdAscBlockCodeAsc(List<Long> sectionIds);

	List<Block> findBySectionIdOrderByBlockCodeAsc(Long sectionId);

	@Query("SELECT b FROM Block b JOIN FETCH b.section s JOIN FETCH b.area a WHERE b.id IN :blockIds")
	List<Block> findAllByIdInWithSectionAndArea(@Param("blockIds") List<Long> blockIds);

	@Query("SELECT b FROM Block b JOIN FETCH b.section s JOIN FETCH b.area a WHERE b.id = :blockId")
	Optional<Block> findByIdWithSectionAndArea(@Param("blockId") Long blockId);

	default Block findByIdWithSectionOrThrow(Long blockId) {
		return findByIdWithSectionAndArea(blockId)
			.orElseThrow(() -> new CustomException(ErrorCode.BLOCK_NOT_FOUND));
	}
}
