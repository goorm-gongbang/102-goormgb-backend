package com.goormgb.be.seat.matchSeat.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.seat.seat.dto.SeatTemplateProjection;
import com.goormgb.be.seat.seat.repository.SeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchSeatPreparationService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final MatchRepository matchRepository;
	private final SeatRepository seatRepository;
	private final Clock clock;
	private final MatchSeatPreparationTransactionalService transactionalService;

	public void prepareMatchSeats() {

		// KST 기준 현재 시각
		ZonedDateTime nowKst = ZonedDateTime.now(clock)
			.withZoneSameInstant(KST);

		// 오늘 기준 +7일 날짜
		LocalDate targetDate = nowKst.toLocalDate().plusDays(7);

		// 7일 뒤 날짜의 00:00 ~ 다음날 00:00 범위
		Instant targetStart = targetDate
			.atStartOfDay(KST)
			.toInstant();

		Instant targetEnd = targetDate
			.plusDays(1)
			.atStartOfDay(KST)
			.toInstant();

		// 생성 대상 경기 조회
		List<Match> matchesToPrepare =
			matchRepository.findBySaleStatusAndMatchAtGreaterThanEqualAndMatchAtLessThan(
				SaleStatus.UPCOMING,
				targetStart,
				targetEnd
			);

		if (matchesToPrepare.isEmpty()) {
			log.info("[MatchSeatPreparationService] 생성 대상 경기 없음");
			return;
		}

		// 좌석 템플릿 조회
		List<SeatTemplateProjection> templates = seatRepository.findAllSeatTemplates();

		if (templates.isEmpty()) {
			log.warn("[MatchSeatPreparationService] 기본 좌석 템플릿이 없어 매치 좌석 생성 스킵");
			return;
		}

		int preparedMatchCount = 0;

		for (Match match : matchesToPrepare) {
			try {
				boolean prepared = transactionalService.prepareSingleMatchSeats(match.getId(), templates);
				if (prepared) {
					preparedMatchCount++;
				}
			} catch (Exception e) {
				log.error("[MatchSeatPreparationService] matchId={} 매치 좌석 생성 실패", match.getId(), e);
			}
		}

		if (preparedMatchCount > 0) {
			log.info("[MatchSeatPreparationService] 매치 좌석 생성 완료: {}건", preparedMatchCount);
		}
	}
}