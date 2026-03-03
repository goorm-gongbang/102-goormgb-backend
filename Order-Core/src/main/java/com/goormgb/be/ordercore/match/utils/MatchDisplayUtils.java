package com.goormgb.be.ordercore.match.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.goormgb.be.ordercore.match.dto.MatchGuideDto;
import com.goormgb.be.ordercore.match.entity.Match;
import com.goormgb.be.ordercore.match.enums.PurchaseStatus;
import com.goormgb.be.ordercore.match.enums.SaleStatus;

@Component
public class MatchDisplayUtils {
	final String DEFAULT_AGE_LIMIT = "전체관람가";
	private static final DateTimeFormatter DATE_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy년 MM월 dd일", Locale.KOREAN);

	private static final DateTimeFormatter TIME_FORMATTER =
			DateTimeFormatter.ofPattern("HH:mm");

	public MatchGuideDto toGuide(Match match) {
		return new MatchGuideDto(
				createTeamsDisplay(match),
				createAgeLimit(),
				createPlaceDisplay(match),
				createAddressDisplay(match),
				createDateTimeDisplay(match),
				createPurchaseStatus(match),
				createMatchDdayLabel(match, LocalDate.now())
		);
	}

	public String createTeamsDisplay(Match match) {
		return match.getHomeClub().getKoName() + " vs " + match.getAwayClub().getKoName();
	}

	public String createAgeLimit() {
		return DEFAULT_AGE_LIMIT;
	}

	public String createPlaceDisplay(Match match) {
		return match.getStadium().getKoName();
	}

	public String createAddressDisplay(Match match) {
		return match.getStadium().getAddress();
	}

	public String createDateTimeDisplay(Match match) {
		var matchAt = match.getMatchAt();

		String dayOfWeek = matchAt.getDayOfWeek()
				.getDisplayName(TextStyle.SHORT, Locale.KOREAN);

		return matchAt.format(DATE_FORMATTER)
				+ " (" + dayOfWeek + ") "
				+ matchAt.format(TIME_FORMATTER);
	}

	public PurchaseStatus createPurchaseStatus(Match match) {
		return match.getSaleStatus() == SaleStatus.ON_SALE
				? PurchaseStatus.PURCHASABLE
				: PurchaseStatus.NOT_PURCHASABLE;
	}

	public String createMatchDdayLabel(Match match, LocalDate today) {
		LocalDate matchDate = match.getMatchAt().toLocalDate();

		long diff = ChronoUnit.DAYS.between(today, matchDate);

		if (diff > 0)
			return "D-" + diff;
		if (diff == 0)
			return "D-DAY";

		return "D+" + Math.abs(diff);
	}
}
