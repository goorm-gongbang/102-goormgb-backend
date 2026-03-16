package com.goormgb.be.seat.seatHold.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
