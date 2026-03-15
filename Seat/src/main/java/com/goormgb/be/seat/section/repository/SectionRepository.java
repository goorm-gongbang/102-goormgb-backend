package com.goormgb.be.seat.section.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.enums.SectionCode;

public interface SectionRepository extends JpaRepository<Section, Long> {

	default Section findByIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id).orElseThrow(() -> new CustomException(errorCode));
	}

	List<Section> findByCode(SectionCode code);

	@Query("""
		select s from Section s
		join fetch s.area a
		order by a.id asc, s.id asc
		""")
	List<Section> findAllWithAreaOrderByAreaIdAscSectionIdAsc();
}
