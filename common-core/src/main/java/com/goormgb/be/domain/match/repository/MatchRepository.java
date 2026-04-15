package com.goormgb.be.domain.match.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;

public interface MatchRepository extends JpaRepository<Match, Long> {
	default Match findByIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id).orElseThrow(() -> new CustomException(errorCode));
	}

	@Query("""
		    select m from Match m
		    join fetch m.homeClub
		    join fetch m.awayClub
		    join fetch m.stadium
		    where m.id = :matchId
		""")
	Optional<Match> findDetailById(@Param("matchId") Long matchId);

	default Match findDetailByIdOrThrow(Long id) {
		return findDetailById(id)
			.orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));
	}

	@EntityGraph(attributePaths = {"homeClub", "awayClub", "stadium"})
	List<Match> findAllByMatchAtGreaterThanEqualAndMatchAtLessThanOrderByMatchAtAsc(
		Instant start,
		Instant end
	);

	List<Match> findBySaleStatus(SaleStatus saleStatus);

	@Modifying
	@Query("UPDATE Match m SET m.saleStatus = :newStatus WHERE m.matchAt < :now AND m.saleStatus <> :newStatus")
	int bulkUpdateEndedMatches(
		@Param("now") Instant now,
		@Param("newStatus") SaleStatus newStatus
	);

	@EntityGraph(attributePaths = {"homeClub", "awayClub"})
	@Query("""
		    select m from Match m
		    where (m.homeClub.id = :clubId or m.awayClub.id = :clubId)
		      and m.matchAt >= :start
		      and m.matchAt < :end
		    order by m.matchAt asc
		""")
	List<Match> findMonthlyByClubId(
		@Param("clubId") Long clubId,
		@Param("start") Instant start,
		@Param("end") Instant end
	);

	List<Match> findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
		SaleStatus saleStatus,
		Instant start,
		Instant end
	);

	/**
	 * 경기 시작 시간이 지난 ON_SALE / SOLD_OUT 경기를 ENDED(예매 마감)로 일괄 전환한다.
	 */
	@Modifying
	@Query("""
		UPDATE Match m
		SET m.saleStatus = :newStatus
		WHERE m.matchAt <= :now
		  AND m.saleStatus IN (:onSale, :soldOut)
		""")
	int bulkUpdateSaleEnded(
		@Param("now") Instant now,
		@Param("newStatus") SaleStatus newStatus,
		@Param("onSale") SaleStatus onSale,
		@Param("soldOut") SaleStatus soldOut
	);

	/**
	 * 경기의 모든 좌석이 SOLD 상태일 때만 ON_SALE -> SOLD_OUT으로 원자적 전환한다.
	 */
	@Modifying
	@Query(
		value = """
			UPDATE matches m
			SET sale_status = 'SOLD_OUT'
			WHERE m.id = :matchId
			  AND m.sale_status = 'ON_SALE'
			  AND NOT EXISTS (
					SELECT 1
					FROM match_seats ms
					WHERE ms.match_id = :matchId
					  AND ms.sale_status <> 'SOLD'
				)
			""",
		nativeQuery = true
	)
	int updateSoldOutIfAllSeatsSold(@Param("matchId") Long matchId);
}
