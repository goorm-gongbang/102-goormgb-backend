package com.goormgb.be.ordercore.club.dto.response;

import java.util.List;

import com.goormgb.be.ordercore.club.entity.Club;

public record ClubGetResponse(
		List<ClubItem> clubs
) {

	public static ClubGetResponse from(List<Club> clubs) {
		return new ClubGetResponse(
				clubs.stream()
						.map(ClubItem::from)
						.toList()
		);
	}

	public record ClubItem(
			Long clubId,
			String koName,
			String enName,
			String logoImg,
			String clubColor
	) {
		public static ClubItem from(Club club) {
			return new ClubItem(
					club.getId(),
					club.getKoName(),
					club.getEnName(),
					club.getLogoImg(),
					club.getClubColor()
			);
		}
	}
}