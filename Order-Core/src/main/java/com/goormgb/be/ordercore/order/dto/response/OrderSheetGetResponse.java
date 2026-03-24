package com.goormgb.be.ordercore.order.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.ordercore.order.query.SeatHoldInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문서 조회 응답")
public record OrderSheetGetResponse(
		@Schema(description = "경기 정보") MatchInfo match,
		@Schema(description = "선점된 좌석 목록") List<SeatInfo> seats,
		@Schema(description = "요약 정보") Summary summary
) {

	@Schema(description = "경기 정보")
	public record MatchInfo(
			@Schema(description = "경기 ID", example = "1") Long matchId,
			@Schema(description = "경기 일시 (UTC)", example = "2026-03-29T05:00:00Z") Instant matchAt,
			@Schema(description = "홈 구단 정보") ClubInfo homeClub,
			@Schema(description = "원정 구단 정보") ClubInfo awayClub,
			@Schema(description = "경기장 정보 (잠실야구장 고정)") StadiumInfo stadium
	) {
		private static final String JAMSIL_STADIUM_NAME = "잠실종합운동장 잠실야구장";
		private static final String JAMSIL_STADIUM_ADDRESS = "서울 송파구 올림픽로 19-2 서울종합운동장";

		public static MatchInfo from(Match match) {
			return new MatchInfo(
					match.getId(),
					match.getMatchAt(),
					new ClubInfo(match.getHomeClub().getId(), match.getHomeClub().getKoName()),
					new ClubInfo(match.getAwayClub().getId(), match.getAwayClub().getKoName()),
					new StadiumInfo(
							match.getStadium().getId(),
							JAMSIL_STADIUM_NAME,
							JAMSIL_STADIUM_ADDRESS
					)
			);
		}
	}

	@Schema(description = "구단 정보")
	public record ClubInfo(
			@Schema(description = "구단 ID", example = "1") Long clubId,
			@Schema(description = "구단 한글명", example = "LG 트윈스") String koName
	) {
	}

	@Schema(description = "경기장 정보")
	public record StadiumInfo(
			@Schema(description = "경기장 ID", example = "1") Long stadiumId,
			@Schema(description = "경기장명", example = "잠실종합운동장 잠실야구장") String koName,
			@Schema(description = "경기장 주소", example = "서울 송파구 올림픽로 19-2 서울종합운동장") String address
	) {
	}

	@Schema(description = "좌석 정보")
	public record SeatInfo(
			@Schema(description = "매치 좌석 ID", example = "149801") Long matchSeatId,
			@Schema(description = "섹션 ID", example = "5") Long sectionId,
			@Schema(description = "섹션명", example = "오렌지석") String sectionName,
			@Schema(description = "블럭 ID", example = "1") Long blockId,
			@Schema(description = "블럭 코드", example = "206") String blockCode,
			@Schema(description = "열 번호", example = "3") Integer rowNo,
			@Schema(description = "좌석 번호", example = "13") Integer seatNo,
			@Schema(description = "성인 기본가 (주중/주말 자동 반영, 원 단위)", example = "20000") Integer adultPrice
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

	@Schema(description = "요약 정보")
	public record Summary(
			@Schema(description = "선점 좌석 수", example = "2") int seatCount,
			@Schema(description = "예매 수수료 (원)", example = "2000") int bookingFee
	) {
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
