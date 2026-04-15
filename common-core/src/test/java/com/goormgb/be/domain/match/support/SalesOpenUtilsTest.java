package com.goormgb.be.domain.match.support;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;

class SalesOpenUtilsTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private final SalesOpenUtils salesOpenUtils = new SalesOpenUtils();

	@Test
	@DisplayName("경기 시작일(KST) 기준 7일 전 11:00 KST 를 UTC Instant 로 반환한다")
	void calculatesOpenAtAsSevenDaysBeforeAt11KST() {
		// matchAt: 2026-04-22 18:30 KST
		Instant matchAt = LocalDateTime.of(2026, 4, 22, 18, 30)
			.atZone(KST).toInstant();
		Match match = Match.builder()
			.matchAt(matchAt)
			.saleStatus(SaleStatus.UPCOMING)
			.build();

		Instant openAt = salesOpenUtils.calculateSalesOpenAt(match);

		Instant expected = LocalDateTime.of(2026, 4, 15, 11, 0)
			.atZone(KST).toInstant();
		assertThat(openAt).isEqualTo(expected);
	}

	@Test
	@DisplayName("경기 시작 시각이 0시에 가까워도 KST 기준 -7일 11:00 으로 계산된다")
	void calculatesOpenAtForEarlyMorningMatch() {
		// matchAt: 2026-05-01 00:30 KST → openAt: 2026-04-24 11:00 KST
		Instant matchAt = LocalDateTime.of(2026, 5, 1, 0, 30)
			.atZone(KST).toInstant();
		Match match = Match.builder().matchAt(matchAt).saleStatus(SaleStatus.UPCOMING).build();

		Instant openAt = salesOpenUtils.calculateSalesOpenAt(match);

		Instant expected = LocalDateTime.of(2026, 4, 24, 11, 0)
			.atZone(KST).toInstant();
		assertThat(openAt).isEqualTo(expected);
	}

	@Test
	@DisplayName("같은 날짜·다른 시각 경기라면 openAt 은 동일하다")
	void samOpenAtForDifferentKickoffTimesOnSameDay() {
		LocalDate matchDate = LocalDate.of(2026, 4, 22);
		Instant earlyMatch = matchDate.atTime(14, 0).atZone(KST).toInstant();
		Instant lateMatch = matchDate.atTime(20, 0).atZone(KST).toInstant();

		Instant a = salesOpenUtils.calculateSalesOpenAt(
			Match.builder().matchAt(earlyMatch).saleStatus(SaleStatus.UPCOMING).build());
		Instant b = salesOpenUtils.calculateSalesOpenAt(
			Match.builder().matchAt(lateMatch).saleStatus(SaleStatus.UPCOMING).build());

		assertThat(a).isEqualTo(b);
	}
}
