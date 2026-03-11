package com.goormgb.be.ordercore.club.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.club.repository.ClubRepository;
import com.goormgb.be.domain.state.repository.TeamSeasonStatsRepository;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.club.dto.response.ClubDetailGetResponse;
import com.goormgb.be.ordercore.club.dto.response.ClubGetResponse;
import com.goormgb.be.ordercore.match.dto.response.ClubMonthlyMatchesResponse;
import com.goormgb.be.ordercore.match.service.MatchService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {
	private final MatchService matchService;
	private final ClubRepository clubRepository;
	private final TeamSeasonStatsRepository teamSeasonStatsRepository;

	public ClubGetResponse getAllClubs() {
		var clubs = clubRepository.findAll();

		Preconditions.validate(!clubs.isEmpty(), ErrorCode.CLUB_NOT_FOUND);

		return ClubGetResponse.from(clubs);
	}

	public ClubDetailGetResponse getClubDetail(Long id) {
		var club = clubRepository.findWithStadiumByIdOrThrow(id, ErrorCode.CLUB_NOT_FOUND);

		int currentYear = LocalDate.now().getYear();
		var stats = teamSeasonStatsRepository
			.findByClubIdAndSeasonYear(id, currentYear)
			.orElse(null);

		return ClubDetailGetResponse.of(club, stats);
	}

	public ClubMonthlyMatchesResponse getClubMonthlyMatches(Long clubId, int year, int month) {
		return matchService.getClubMonthlyMatches(clubId, year, month);
	}
}
