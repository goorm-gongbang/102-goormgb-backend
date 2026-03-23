package com.goormgb.be.seat.matchSeat.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchSeatCleanupService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final long RETENTION_DAYS = 7L;

	private final MatchSeatRepository matchSeatRepository;
	private final Clock clock;

	@Transactional
	public void cleanupEndedMatchSeats() {
		Instant cutoff = calculateCutoff();
		List<Long> cleanupTargetMatchSeatIds = matchSeatRepository.findCleanupTargetMatchSeatIds(
			SaleStatus.ENDED.name(),
			cutoff
		);

		if (cleanupTargetMatchSeatIds.isEmpty()) {
			log.info("[MatchSeatCleanupService] 삭제 대상 match_seat 없음. cutoff={}", cutoff);
			return;
		}

		int deletedCount = matchSeatRepository.deleteByIdIn(cleanupTargetMatchSeatIds);
		log.info(
			"[MatchSeatCleanupService] 종료 경기 match_seat 정리 완료. cutoff={}, deletedCount={}",
			cutoff,
			deletedCount
		);
	}

	Instant calculateCutoff() {
		LocalDate todayKst = clock.instant().atZone(KST).toLocalDate();
		return todayKst.minusDays(RETENTION_DAYS).atStartOfDay(KST).toInstant();
	}
}