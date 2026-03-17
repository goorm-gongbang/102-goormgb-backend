package com.goormgb.be.seat.matchSeat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

	@Query("""
		SELECT ms FROM MatchSeat ms
		WHERE ms.matchId = :matchId
		  AND ms.blockId = :blockId
		  AND ms.saleStatus = com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus.AVAILABLE
		ORDER BY ms.rowNo ASC, ms.templateColNo ASC
		""")
	List<MatchSeat> findAvailableSeatsByMatchIdAndBlockId(
		@Param("matchId") Long matchId,
		@Param("blockId") Long blockId
	);

	@Query("""
		SELECT ms FROM MatchSeat ms
		WHERE ms.matchId = :matchId
		  AND ms.sectionId = :sectionId
		ORDER BY ms.blockId ASC, ms.rowNo ASC, ms.seatNo ASC
		""")
	List<MatchSeat> findByMatchIdAndSectionIdOrderByBlockIdAscRowNoAscSeatNoAsc(
		@Param("matchId") Long matchId,
		@Param("sectionId") Long sectionId
	);

	List<MatchSeat> findAllByMatchIdAndSeatIdIn(Long matchId, List<Long> seatIds);

	/**
	 * 좌석이 AVAILABLE 상태일 때만 BLOCKED로 변경한다.
	 *
	 * <p>DB row-level lock이 원자성을 보장하므로 동시 요청 시에도 1명만 성공한다.
	 * 반환값이 0이면 다른 유저가 이미 선점한 것으로 판단한다.</p>
	 *
	 * @return 변경된 행 수 (0 또는 1)
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
		UPDATE MatchSeat ms
		SET ms.saleStatus = com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus.BLOCKED
		WHERE ms.id = :matchSeatId
		  AND ms.saleStatus = com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus.AVAILABLE
		""")
	int markBlockedIfAvailable(@Param("matchSeatId") Long matchSeatId);

	/**
	 * BLOCKED 상태인 좌석들을 일괄로 AVAILABLE로 복원한다.
	 *
	 * <p>충돌 감지 시 이미 BLOCKED로 변경한 좌석들을 한 번의 쿼리로 롤백한다.</p>
	 *
	 * @return 변경된 행 수
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
		UPDATE MatchSeat ms
		SET ms.saleStatus = com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus.AVAILABLE
		WHERE ms.id IN :matchSeatIds
		  AND ms.saleStatus = com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus.BLOCKED
		""")
	int markAvailableIfBlockedInBatch(@Param("matchSeatIds") List<Long> matchSeatIds);
}
