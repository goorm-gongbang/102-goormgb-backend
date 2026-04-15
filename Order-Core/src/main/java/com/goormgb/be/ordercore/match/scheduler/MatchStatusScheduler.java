package com.goormgb.be.ordercore.match.scheduler;

import java.time.Instant;
import java.time.LocalDate;
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
import com.goormgb.be.domain.match.support.SalesOpenUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchStatusScheduler {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final MatchRepository matchRepository;
	private final SalesOpenUtils salesOpenUtils;

	/**
	 * 매일 오전 10:59 (KST): 오늘(KST) 이 판매 오픈일인 UPCOMING 경기를 ON_SALE 로 전환한다.
	 *
	 * <p>판매 오픈 조건: 경기 7일 전 오전 11시 ({@link SalesOpenUtils} 기준).</p>
	 *
	 * <p>판정을 시각 비교({@code now >= openAt}) 대신 <b>KST 기준 날짜 비교</b> 로 수행한다.
	 * 이유는 cron 이 10:59 에 발화했을 때 {@code now(10:59)} 는 {@code openAt(11:00)} 보다
	 * 1분 이르므로 시각 비교 방식이면 오늘 오픈해야 할 경기가 전환되지 않고
	 * 다음 날 10:59 에야 처리되는 "하루 지연" 문제가 있기 때문이다.
	 * 하루 5경기·매일 서로 다른 경기 날짜라는 운영 특성상 openAt 을 날짜 단위로만
	 * 판정해도 정확하다.</p>
	 *
	 * <p>본 스케줄러는 화면 표기(예매 가능/불가 라벨) 및 {@code sale_status} 의존 쿼리
	 * (예: 당일 매진 시 ON_SALE → SOLD_OUT 원자 전환) 의 사전 반영을 담당한다. 실제
	 * 대기열 진입 허용 판정은 Queue 모듈의 Lazy 시간 비교({@code now >= openAt}) 가
	 * 담당하므로 본 스케줄러가 실패·지연되어도 11시 정각 진입 자체는 영향받지 않는다.</p>
	 *
	 * <p>{@code openDate <= today} 로 판정하여 과거에 스케줄러가 누락됐던 경기(UPCOMING
	 * 으로 남아있는 지난 오픈일 경기) 도 동일 트랜잭션에서 복구 전환된다.</p>
	 */
	@Scheduled(cron = "0 59 10 * * *", zone = "Asia/Seoul")
	@Transactional
	public void openSales() {
		LocalDate today = LocalDate.now(KST);
		List<Match> upcomingMatches = matchRepository.findBySaleStatus(SaleStatus.UPCOMING);

		int count = 0;
		for (Match match : upcomingMatches) {
			LocalDate openDate = salesOpenUtils.calculateSalesOpenAt(match).atZone(KST).toLocalDate();
			if (!openDate.isAfter(today)) {
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
		Instant startOfToday = ZonedDateTime.now(KST)
			.truncatedTo(ChronoUnit.DAYS)
			.toInstant();
		int count = matchRepository.bulkUpdateEndedMatches(startOfToday, SaleStatus.ENDED);

		if (count > 0) {
			log.info("[MatchStatusScheduler] Match status → ENDED 전환 완료: {}건", count);
		}
	}
}
