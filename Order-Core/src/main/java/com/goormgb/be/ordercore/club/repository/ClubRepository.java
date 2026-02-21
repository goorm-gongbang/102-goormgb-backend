package com.goormgb.be.ordercore.club.repository;

import com.goormgb.be.ordercore.club.dto.response.ClubDetailFlatDto;
import com.goormgb.be.ordercore.club.dto.response.ClubListItemResponse;
import com.goormgb.be.ordercore.club.entity.Club;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {

    List<ClubListItemResponse> findAllClubList();

    @EntityGraph(attributePaths = {"stadium"})
    Optional<Club> findWithStadiumById(Long id);

    /**
     * FR-C02 구단 상세 기본 정보 조회용 쿼리.
     * DTO Projection 방식으로 단건 조회 성능을 최적화.
     */
    List<ClubDetailFlatDto> findClubDetailFlat(@Param("clubId") Long clubId);
}
