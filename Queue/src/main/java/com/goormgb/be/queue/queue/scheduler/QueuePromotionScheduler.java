package com.goormgb.be.queue.queue.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.goormgb.be.queue.config.QueueProperties;
import com.goormgb.be.queue.queue.service.QueueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueuePromotionScheduler {

	private final QueueService queueService;
	private final QueueProperties queueProperties;

	// 활성 경기 목록을 짧은 간격으로 순회하면서 WAITING -> READY 승급을 수행한다.
	@Scheduled(fixedDelayString = "${queue.promotion-interval-ms}")
	public void promoteWaitingUsers() {
		try {
			queueService.promoteActiveMatches();
		} catch (Exception e) {
			log.error(
				"Queue promotion scheduler failed. intervalMs={}",
				queueProperties.promotionIntervalMs(),
				e
			);
		}
	}
}
