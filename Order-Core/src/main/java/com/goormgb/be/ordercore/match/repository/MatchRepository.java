package com.goormgb.be.ordercore.match.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.match.entity.Match;

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
			LocalDateTime start,
			LocalDateTime end
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
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);
}
