package com.goormgb.be.seat.recommendation.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.goormgb.be.seat.metrics.SeatMetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redisson 기반 블럭 단위 분산 락.
 *
 * <p>좌석 배정 시 동일 경기의 동일 블럭에 대한 동시 접근을 방지한다.
 * 락 키에 matchId를 포함하여 서로 다른 경기의 같은 blockId가 간섭하지 않도록 한다.
 * 락 획득에 실패하면 다른 사용자가 해당 블럭에서 좌석을 선택 중임을 의미한다.</p>
 *
 * <ul>
 *   <li>waitTime(3초): 락 획득 대기 시간 — 앞 사용자 처리 완료를 기다림</li>
 *   <li>Watchdog: 작업 완료 전까지 락을 자동 연장 (기본 30초, 자동 갱신)</li>
 *   <li>소유자 검증: 락을 획득한 스레드만 해제 가능</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatBlockLock {

	private final SeatMetricsService seatMetricsService;

	private static final String LOCK_KEY_FORMAT = "seat:recommendation:match:%d:block:%d";
	private static final long WAIT_TIME_SECONDS = 3;

	private final RedissonClient redissonClient;

	public boolean tryLock(Long matchId, Long blockId) {
		RLock lock = redissonClient.getLock(buildKey(matchId, blockId));
		long start = System.nanoTime();

		try {
			return lock.tryLock(WAIT_TIME_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} finally {
			// 분산락 획득 대기 시간 기록 (락 경합 및 대기 지연 분석용)
			seatMetricsService.recordLockWaitTime(
				Duration.ofNanos(System.nanoTime() - start)
			);
		}
	}

	public void unlock(Long matchId, Long blockId) {
		RLock lock = redissonClient.getLock(buildKey(matchId, blockId));
		if (lock.isHeldByCurrentThread()) {
			try {
				lock.unlock();
			} catch (Exception e) {
				log.error("블럭 분산 락 해제 실패 - matchId: {}, blockId: {}", matchId, blockId, e);
			}
		}
	}

	private String buildKey(Long matchId, Long blockId) {
		return String.format(LOCK_KEY_FORMAT, matchId, blockId);
	}
}
