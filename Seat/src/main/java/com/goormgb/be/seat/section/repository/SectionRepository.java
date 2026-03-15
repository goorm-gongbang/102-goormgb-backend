package com.goormgb.be.seat.section.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.enums.SectionCode;

public interface SectionRepository extends JpaRepository<Section, Long> {

	List<Section> findByCode(SectionCode code);
}
