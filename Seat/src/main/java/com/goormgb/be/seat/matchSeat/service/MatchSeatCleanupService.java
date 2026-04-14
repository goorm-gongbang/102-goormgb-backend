package com.goormgb.be.seat.matchSeat.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
	// IN 절 파라미터 수를 과도하게 키우지 않기 위한 안전한 기본 배치 크기
	private static final int DELETE_BATCH_SIZE = 1_000;

	private final MatchSeatRepository matchSeatRepository;
	private final Clock clock;

	@Transactional
	public void cleanupEndedMatchSeats() {
		Instant cutoff = calculateCutoff();
		log.info("[MatchSeatCleanupService] 클린업 시작. at={}, cutoff={}",
			LocalDateTime.now(KST), cutoff);

		long queryStart = System.currentTimeMillis();
		List<Long> cleanupTargetMatchSeatIds = matchSeatRepository.findCleanupTargetMatchSeatIds(
			SaleStatus.ENDED.name(),
			cutoff
		);
		log.info("[MatchSeatCleanupService] findCleanupTargetIds 완료. at={}, targetCount={}, elapsed={}ms",
			LocalDateTime.now(KST), cleanupTargetMatchSeatIds.size(), System.currentTimeMillis() - queryStart);

		if (cleanupTargetMatchSeatIds.isEmpty()) {
			log.info("[MatchSeatCleanupService] 삭제 대상 match_seat 없음. cutoff={}", cutoff);
			return;
		}

		long deleteStart = System.currentTimeMillis();
		int deletedCount = deleteInBatches(cleanupTargetMatchSeatIds);
		log.info(
			"[MatchSeatCleanupService] 종료 경기 match_seat 정리 완료. at={}, cutoff={}, deletedCount={}, deleteElapsed={}ms, totalElapsed={}ms",
			LocalDateTime.now(KST), cutoff, deletedCount,
			System.currentTimeMillis() - deleteStart,
			System.currentTimeMillis() - queryStart
		);
	}

	Instant calculateCutoff() {
		LocalDate todayKst = clock.instant().atZone(KST).toLocalDate();
		return todayKst.minusDays(RETENTION_DAYS).atStartOfDay(KST).toInstant();
	}

	private int deleteInBatches(List<Long> cleanupTargetMatchSeatIds) {
		int totalDeletedCount = 0;

		for (int i = 0; i < cleanupTargetMatchSeatIds.size(); i += DELETE_BATCH_SIZE) {
			List<Long> batch = cleanupTargetMatchSeatIds.subList(
				i,
				Math.min(i + DELETE_BATCH_SIZE, cleanupTargetMatchSeatIds.size())
			);
			totalDeletedCount += matchSeatRepository.deleteByIdIn(batch);
		}

		return totalDeletedCount;
	}
}