package com.goormgb.be.domain.match.support;

import java.time.Instant;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;

/**
 * 경기 판매 오픈 시각(openAt) 을 계산하는 도메인 유틸.
 *
 * <p>정책: 경기 시작일(KST) 기준 {@value #SALES_OPEN_DAYS_BEFORE_MATCH} 일 전
 * {@value #SALES_OPEN_HOUR}:{@value #SALES_OPEN_MINUTE} (KST) 에 판매를 오픈한다.</p>
 *
 * <p>본 유틸은 {@code common-core} 로 승격되어 Queue / Order-Core 등 모듈 간 공통으로 사용된다.
 * 대기열 진입 판정은 DB {@code sale_status} 컬럼이 아닌 본 유틸이 계산한 openAt 과
 * 현재 시각을 비교하는 Lazy 방식으로 수행된다.</p>
 */
@Component
public class SalesOpenUtils {
	private static final int SALES_OPEN_DAYS_BEFORE_MATCH = 7;
	private static final int SALES_OPEN_HOUR = 11;
	private static final int SALES_OPEN_MINUTE = 0;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/**
	 * 주어진 경기의 판매 오픈 시각을 UTC {@link Instant} 로 반환한다.
	 *
	 * @param match 대상 경기
	 * @return 경기 시작일(KST) 기준 7일 전 11:00 KST 시각의 UTC Instant
	 */
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

	/**
	 * 주어진 시각 기준으로 경기가 구매/대기열 진입 가능 상태인지 판정한다.
	 *
	 * <p>판정 기준:
	 * <ul>
	 *   <li>{@code now >= openAt} — 판매 오픈 시각이 도래했는지</li>
	 *   <li>{@link SaleStatus#ENDED} / {@link SaleStatus#SOLD_OUT} 은 불가</li>
	 * </ul>
	 *
	 * <p>본 메서드는 Queue 의 대기열 진입 판정과 Order-Core 의 화면 표기 판정
	 * 양쪽에서 동일 기준으로 사용되어 프론트·백엔드 간 판정 불일치를 방지한다.</p>
	 *
	 * @param match 대상 경기
	 * @param now 판정 기준 시각
	 * @return 구매 가능이면 true
	 */
	public boolean isPurchasable(Match match, Instant now) {
		Instant openAt = calculateSalesOpenAt(match);
		return !now.isBefore(openAt)
			&& match.getSaleStatus() != SaleStatus.ENDED
			&& match.getSaleStatus() != SaleStatus.SOLD_OUT;
	}
}
