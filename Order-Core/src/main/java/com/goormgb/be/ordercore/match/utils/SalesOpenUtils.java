package com.goormgb.be.ordercore.match.utils;

import java.time.Instant;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.goormgb.be.domain.match.entity.Match;

@Component
public class SalesOpenUtils {
	private static final int SALES_OPEN_DAYS_BEFORE_MATCH = 7;
	private static final int SALES_OPEN_HOUR = 11;
	private static final int SALES_OPEN_MINUTE = 0;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public Instant calculateSalesOpenAt(Match match) {
		return match.getMatchAt()
			.atZone(KST)
			.minusDays(SALES_OPEN_DAYS_BEFORE_MATCH)
			.withHour(SALES_OPEN_HOUR)
			.withMinute(SALES_OPEN_MINUTE)
			.withSecond(0)
			.withNano(0)
			.toInstant();
	}
}
