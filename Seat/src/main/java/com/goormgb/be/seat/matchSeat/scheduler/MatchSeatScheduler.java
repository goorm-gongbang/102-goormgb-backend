package com.goormgb.be.seat.matchSeat.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.goormgb.be.seat.matchSeat.service.MatchSeatPreparationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSeatScheduler {

	private final MatchSeatPreparationService matchSeatPreparationService;

	/**
	 * 매일 자정 00:00: 경기 7일 전이 된 UPCOMING 경기에 대해 match_seats 데이터를 미리 생성한다.
	 */
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void prepareMatchSeats() {
		matchSeatPreparationService.prepareMatchSeats();
	}
}
