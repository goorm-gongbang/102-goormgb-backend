package com.goormgb.be.seat.seat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.goormgb.be.seat.seat.dto.SeatTemplateProjection;
import com.goormgb.be.seat.seat.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	@Query("""
			select
				s.id as seatId,
				b.area.id as areaId,
				b.section.id as sectionId,
				b.id as blockId,
				s.rowNo as rowNo,
				s.seatNo as seatNo,
				s.templateColNo as templateColNo,
				s.seatZone as seatZone
			from Seat s
			join s.block b
		""")
	List<SeatTemplateProjection> findAllSeatTemplates();
}
