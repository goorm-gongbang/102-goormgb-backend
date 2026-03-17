package com.goormgb.be.seat.seatHold.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.seat.seatHold.entity.SeatHold;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

	List<SeatHold> findAllByUserIdAndMatchId(Long userId, Long matchId);

	void deleteAllByMatchSeatIdIn(List<Long> matchSeatIds);

	List<SeatHold> findAllByMatchIdAndExpiresAtAfter(Long matchId, Instant now);

	List<SeatHold> findAllByMatchIdAndMatchSeatIdInAndExpiresAtAfter(
		Long matchId,
		List<Long> matchSeatIds,
		Instant now
	);

	List<SeatHold> findAllByUserIdAndMatchIdAndExpiresAtAfter(Long userId, Long matchId, Instant now);

	List<SeatHold> findAllByMatchIdAndSeatIdInAndExpiresAtAfter(Long matchId, List<Long> seatIds, Instant now);

	@Query("SELECT sh.matchSeatId FROM SeatHold sh WHERE sh.expiresAt < :now")
	List<Long> findExpiredMatchSeatIds(@Param("now") Instant now);

	@Modifying
	@Query("DELETE FROM SeatHold sh WHERE sh.expiresAt < :now")
	int deleteExpiredHolds(@Param("now") Instant now);
}
