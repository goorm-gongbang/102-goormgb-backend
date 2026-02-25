package com.goormgb.be.ordercore.club.service;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.club.dto.response.ClubDetailGetResponse;
import com.goormgb.be.ordercore.club.dto.response.ClubGetResponse;
import com.goormgb.be.ordercore.club.repository.ClubRepository;
import com.goormgb.be.ordercore.state.repository.TeamSeasonStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {
    final private ClubRepository clubRepository;
    final private TeamSeasonStatsRepository teamSeasonStatsRepository;

    public List<ClubGetResponse> getAllClubs(){
        var clubs = clubRepository.findAll();

        Preconditions.validate(!clubs.isEmpty(), ErrorCode.CLUB_NOT_FOUND);

        return clubs.stream()
                .map(ClubGetResponse::from)
                .toList();
    }

    public ClubDetailGetResponse getClubDetail(Long id) {
        var club = clubRepository.findWithStadiumByIdOrThrow(id, ErrorCode.CLUB_NOT_FOUND);

        int currentYear = LocalDate.now().getYear();
        var stats = teamSeasonStatsRepository
                .findByClubIdAndSeasonYear(id, currentYear)
                .orElse(null);

        return ClubDetailGetResponse.of(club, stats);
    }

}
