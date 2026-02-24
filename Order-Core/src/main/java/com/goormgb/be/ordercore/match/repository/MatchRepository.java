package com.goormgb.be.ordercore.match.repository;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.match.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    default Match findByOrThrow(Long id, ErrorCode errorCode) {
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
}
