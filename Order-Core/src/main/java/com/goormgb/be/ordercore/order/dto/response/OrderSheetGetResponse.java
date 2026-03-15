package com.goormgb.be.ordercore.order.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.ordercore.order.query.SeatHoldInfo;

public record OrderSheetGetResponse(
		MatchInfo match,
		List<SeatInfo> seats,
		Summary summary
) {

	public record MatchInfo(
			Long matchId,
			Instant matchAt,
			ClubInfo homeClub,
			ClubInfo awayClub,
			StadiumInfo stadium
	) {
		public static MatchInfo from(Match match) {
			return new MatchInfo(
					match.getId(),
					match.getMatchAt(),
					new ClubInfo(match.getHomeClub().getId(), match.getHomeClub().getKoName()),
					new ClubInfo(match.getAwayClub().getId(), match.getAwayClub().getKoName()),
					new StadiumInfo(
							match.getStadium().getId(),
							match.getStadium().getKoName(),
							match.getStadium().getAddress()
					)
			);
		}
	}

	public record ClubInfo(Long clubId, String koName) {
	}

	public record StadiumInfo(Long stadiumId, String koName, String address) {
	}

	public record SeatInfo(
			Long matchSeatId,
			Long sectionId,
			String sectionName,
			Long blockId,
			String blockCode,
			Integer rowNo,
			Integer seatNo,
			Integer adultPrice
	) {
		public static SeatInfo of(SeatHoldInfo holdInfo, Integer adultPrice) {
			return new SeatInfo(
					holdInfo.matchSeatId(),
					holdInfo.sectionId(),
					holdInfo.sectionName(),
					holdInfo.blockId(),
					holdInfo.blockCode(),
					holdInfo.rowNo(),
					holdInfo.seatNo(),
					adultPrice
			);
		}
	}

	public record Summary(int seatCount, int bookingFee) {
	}

	private static final int BOOKING_FEE = 2_000;

	public static OrderSheetGetResponse of(Match match, List<SeatInfo> seats) {
		return new OrderSheetGetResponse(
				MatchInfo.from(match),
				seats,
				new Summary(seats.size(), BOOKING_FEE)
		);
	}
}
