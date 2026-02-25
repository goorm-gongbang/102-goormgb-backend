package com.goormgb.be.ordercore.state.repository;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.state.entity.TeamSeasonStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamSeasonStatsRepository extends JpaRepository<TeamSeasonStats, Long> {

//    Optional<TeamSeasonStats> findFirstByClubIdOrderBySeasonYearDesc(Long clubId);
//
    Optional<TeamSeasonStats> findByClubIdAndSeasonYear(Long clubId, int seasonYear);

    default TeamSeasonStats findByIdOrThrow(Long id, ErrorCode errorCode) {
        return findById(id).orElseThrow(() -> new CustomException(errorCode));
    }

}
