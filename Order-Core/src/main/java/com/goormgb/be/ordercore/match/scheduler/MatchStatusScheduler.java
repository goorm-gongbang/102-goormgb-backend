package com.goormgb.be.ordercore.match.scheduler;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.ordercore.match.utils.SalesOpenUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchStatusScheduler {

	private final MatchRepository matchRepository;
	private final SalesOpenUtils salesOpenUtils;

	/**
	 * 매일 오전 11시: UPCOMING 경기 중 오늘 판매 오픈 대상인 경기를 ON_SALE로 전환한다.
	 * 판매 오픈 조건: 경기 7일 전 오전 11시 (SalesOpenUtils 기준)
	 */
	@Scheduled(cron = "0 0 11 * * *", zone = "Asia/Seoul")
	@Transactional
	public void openSales() {
		Instant now = Instant.now();
		List<Match> upcomingMatches = matchRepository.findBySaleStatus(SaleStatus.UPCOMING);

		int count = 0;
		for (Match match : upcomingMatches) {
			Instant salesOpenAt = salesOpenUtils.calculateSalesOpenAt(match);
			if (!now.isBefore(salesOpenAt)) {
				match.updateSaleStatus(SaleStatus.ON_SALE);
				count++;
			}
		}

		if (count > 0) {
			log.info("[MatchStatusScheduler] UPCOMING → ON_SALE 전환 완료: {}건", count);
		}
	}

	/**
	 * 매 정시: 경기 시작 시간이 지난 ON_SALE / SOLD_OUT 경기를 ENDED(예매 마감)로 전환한다.
	 * 경기 시작 이후에는 티켓 판매가 불가능해야 한다.
	 */
	@Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
	@Transactional
	public void closeSales() {
		Instant now = Instant.now();
		int count = matchRepository.bulkUpdateSaleEnded(
			now, SaleStatus.ENDED, SaleStatus.ON_SALE, SaleStatus.SOLD_OUT
		);

		if (count > 0) {
			log.info("[MatchStatusScheduler] ON_SALE/SOLD_OUT → ENDED 전환 완료: {}건", count);
		}
	}

	/**
	 * 매일 자정 00:00: 전날까지 경기가 있던 건을 ENDED로 전환한다.
	 * 예) 3월 10일 경기 → 3월 11일 00:00에 ENDED 처리
	 */
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	@Transactional
	public void closeEndedMatches() {
		Instant startOfToday = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
			.truncatedTo(ChronoUnit.DAYS)
			.toInstant();
		int count = matchRepository.bulkUpdateEndedMatches(startOfToday, SaleStatus.ENDED);

		if (count > 0) {
			log.info("[MatchStatusScheduler] Match status → ENDED 전환 완료: {}건", count);
		}
	}
}
