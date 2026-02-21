package com.goormgb.be.ordercore.club.repository;

import com.goormgb.be.ordercore.club.dto.response.ClubListItemResponse;
import com.goormgb.be.ordercore.club.entity.Club;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {

    List<ClubListItemResponse> findAllClubList();

    @EntityGraph(attributePaths = {"stadium"})
    Optional<Club> findWithStadiumById(Long id);
}
