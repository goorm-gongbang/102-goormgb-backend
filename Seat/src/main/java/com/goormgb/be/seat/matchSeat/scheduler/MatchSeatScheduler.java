package com.goormgb.be.seat.matchSeat.scheduler;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.goormgb.be.seat.matchSeat.service.MatchSeatCleanupService;
import com.goormgb.be.seat.matchSeat.service.MatchSeatPreparationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSeatScheduler {

	private final MatchSeatPreparationService matchSeatPreparationService;
	private final MatchSeatCleanupService matchSeatCleanupService;

	/**
	 * 매일 자정 00:00: 경기 7일 전이 된 UPCOMING 경기에 대해 match_seats 데이터를 미리 생성한다.
	 */
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void prepareMatchSeats() {
		log.info("[MatchSeatScheduler] 좌석 생성 스케줄러 실행 시각(KST): {}", ZonedDateTime.now(ZoneId.of("Asia/Seoul")));
		matchSeatPreparationService.prepareMatchSeats();
	}

	/**
	 * 매일 오전 01:00: 종료 후 7일이 지난 경기의 match_seat 데이터를 정리한다.
	 */
	@Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
	public void cleanupEndedMatchSeats() {
		log.info("[MatchSeatCleanupScheduler] 좌석 삭제 스케줄러 실행 시각(KST): {}", ZonedDateTime.now(ZoneId.of("Asia/Seoul")));
		matchSeatCleanupService.cleanupEndedMatchSeats();
	}
}
