package com.goormgb.be.domain.state.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.domain.state.entity.TeamSeasonStats;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;

public interface TeamSeasonStatsRepository extends JpaRepository<TeamSeasonStats, Long> {

	//    Optional<TeamSeasonStats> findFirstByClubIdOrderBySeasonYearDesc(Long clubId);
	//
	Optional<TeamSeasonStats> findByClubIdAndSeasonYear(Long clubId, int seasonYear);

	default TeamSeasonStats findByIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id).orElseThrow(() -> new CustomException(errorCode));
	}

}
