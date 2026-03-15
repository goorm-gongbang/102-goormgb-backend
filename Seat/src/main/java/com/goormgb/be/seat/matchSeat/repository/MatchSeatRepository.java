package com.goormgb.be.seat.matchSeat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;

public interface MatchSeatRepository extends JpaRepository<MatchSeat, Long> {
	boolean existsByMatchId(Long matchId);

	@Query("""
		select ms.sectionId as sectionId, count(ms) as remainingSeatCount
		from MatchSeat ms
		where ms.matchId = :matchId
		  and ms.saleStatus = :saleStatus
		group by ms.sectionId
		""")
	List<SectionRemainingSeatProjection> countRemainingSeatsByMatchIdAndSaleStatusGroupBySectionId(
		@Param("matchId") Long matchId,
		@Param("saleStatus") MatchSeatSaleStatus saleStatus
	);
}
