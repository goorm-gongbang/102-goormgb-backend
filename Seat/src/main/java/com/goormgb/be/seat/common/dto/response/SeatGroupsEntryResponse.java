package com.goormgb.be.seat.common.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.stadium.entity.Stadium;
import com.goormgb.be.seat.redis.SeatSession;

public record SeatGroupsEntryResponse(
	MatchInfo match,
	SeatSessionInfo seatSession,
	List<SeatGroupInfo> seatGroups
) {

	public static SeatGroupsEntryResponse of(Match match, SeatSession seatPreferenceCache,
		List<SeatGroupInfo> seatGroups) {
		return new SeatGroupsEntryResponse(
			MatchInfo.from(match),
			SeatSessionInfo.from(seatPreferenceCache),
			seatGroups
		);
	}

	public record MatchInfo(
		Long matchId,
		ClubInfo homeClub,
		ClubInfo awayClub,
		Instant matchAt,
		StadiumInfo stadium
	) {

		public static MatchInfo from(Match match) {
			return new MatchInfo(
				match.getId(),
				ClubInfo.from(match.getHomeClub()),
				ClubInfo.from(match.getAwayClub()),
				match.getMatchAt(),
				StadiumInfo.from(match.getStadium())
			);
		}
	}

	public record ClubInfo(
		Long clubId,
		String koName,
		String logoImg
	) {

		public static ClubInfo from(Club club) {
			return new ClubInfo(
				club.getId(),
				club.getKoName(),
				club.getLogoImg()
			);
		}
	}

	public record StadiumInfo(
		Long stadiumId,
		String koName
	) {

		public static StadiumInfo from(Stadium stadium) {
			return new StadiumInfo(
				stadium.getId(),
				stadium.getKoName()
			);
		}
	}

	public record SeatSessionInfo(
		boolean recommendationEnabled,
		int ticketCount
	) {

		public static SeatSessionInfo from(SeatSession seatPreferenceCache) {
			return new SeatSessionInfo(
				seatPreferenceCache.isRecommendationEnabled(),
				seatPreferenceCache.getTicketCount()
			);
		}
	}

	public record SeatGroupInfo(
		Long areaId,
		String areaName,
		List<SectionInfo> sections
	) {
	}

	public record SectionInfo(
		Long sectionId,
		String sectionName,
		String displayName,
		List<Long> blockIds,
		long remainingSeatCount
	) {
	}
}