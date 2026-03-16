package com.goormgb.be.seat.recommendation.service;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Redisson 기반 블럭 단위 분산 락.
 *
 * <p>좌석 배정 시 동일 블럭에 대한 동시 접근을 방지한다.
 * 락 획득에 실패하면 다른 사용자가 해당 블럭에서 좌석을 선택 중임을 의미한다.</p>
 *
 * <ul>
 *   <li>waitTime(3초): 락 획득 대기 시간 — 앞 사용자 처리 완료를 기다림</li>
 *   <li>leaseTime(5초): 락 임대 시간 — 작업 완료 전 자동 해제 방지</li>
 *   <li>소유자 검증: 락을 획득한 스레드만 해제 가능</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SeatBlockLock {

	private static final String LOCK_KEY_PREFIX = "block_lock:";
	private static final long WAIT_TIME_SECONDS = 3;
	private static final long LEASE_TIME_SECONDS = 5;

	private final RedissonClient redissonClient;

	public boolean tryLock(Long blockId) {
		RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + blockId);
		try {
			return lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public void unlock(Long blockId) {
		RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + blockId);
		if (lock.isHeldByCurrentThread()) {
			lock.unlock();
		}
	}
}
