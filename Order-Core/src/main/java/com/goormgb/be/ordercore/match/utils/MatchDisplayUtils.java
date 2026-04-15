package com.goormgb.be.ordercore.match.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.PurchaseStatus;
import com.goormgb.be.domain.match.support.SalesOpenUtils;
import com.goormgb.be.ordercore.match.dto.MatchGuideDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MatchDisplayUtils {

	private final SalesOpenUtils salesOpenUtils;

	private static final String DEFAULT_AGE_LIMIT = "전체관람가";

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
		ZonedDateTime matchAt = match.getMatchAt().atZone(ZoneId.of("Asia/Seoul"));

		String dayOfWeek = matchAt.getDayOfWeek()
			.getDisplayName(TextStyle.SHORT, Locale.KOREAN);

		return matchAt.format(DATE_FORMATTER)
			+ " (" + dayOfWeek + ") "
			+ matchAt.format(TIME_FORMATTER);
	}

	/**
	 * 화면 표기용 구매 가능 여부를 반환한다.
	 *
	 * <p>Queue 의 진입 판정과 기준을 일치시키기 위해
	 * {@link SalesOpenUtils#isPurchasable(Match, Instant)} 에 판정을 위임한다.
	 * {@code sale_status} 스케줄러 반영이 지연되더라도 화면과 대기열 판정이
	 * 일관되게 유지되도록 한다.</p>
	 */
	public PurchaseStatus createPurchaseStatus(Match match) {
		return salesOpenUtils.isPurchasable(match, Instant.now())
			? PurchaseStatus.PURCHASABLE
			: PurchaseStatus.NOT_PURCHASABLE;
	}

	public String createMatchDdayLabel(Match match, LocalDate today) {
		LocalDate matchDate = match.getMatchAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();

		long diff = ChronoUnit.DAYS.between(today, matchDate);

		if (diff > 0)
			return "D-" + diff;
		if (diff == 0)
			return "D-DAY";

		return "D+" + Math.abs(diff);
	}
}
