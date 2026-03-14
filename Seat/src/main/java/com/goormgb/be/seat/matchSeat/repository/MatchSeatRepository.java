package com.goormgb.be.seat.matchSeat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.seat.matchSeat.entity.MatchSeat;

public interface MatchSeatRepository extends JpaRepository<MatchSeat, Long> {
	boolean existsByMatchId(Long matchId);
}
