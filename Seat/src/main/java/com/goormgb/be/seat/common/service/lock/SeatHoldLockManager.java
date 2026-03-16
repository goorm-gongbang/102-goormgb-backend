package com.goormgb.be.seat.common.service.lock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SeatHoldLockManager {

	private static final String KEY_PREFIX = "seat:hold:match:";
	private static final Duration WAIT_TIME = Duration.ofMillis(500);
	private static final Duration LEASE_TIME = Duration.ofSeconds(5);

	private final RedissonClient redissonClient;

	public List<RLock> lockAll(Long matchId, List<Long> sortedSeatIds) {
		List<RLock> acquired = new ArrayList<>();

		try {
			for (Long seatId : sortedSeatIds) {
				RLock lock = redissonClient.getLock(buildKey(matchId, seatId));
				boolean locked = lock.tryLock(WAIT_TIME.toMillis(), LEASE_TIME.toMillis(), TimeUnit.MILLISECONDS);
				if (!locked) {
					unlockAll(acquired);
					throw new CustomException(ErrorCode.SEAT_LOCK_ACQUISITION_FAILED);
				}
				acquired.add(lock);
			}
			return acquired;
		} catch (InterruptedException e) {
			unlockAll(acquired);
			Thread.currentThread().interrupt();
			throw new CustomException(ErrorCode.SEAT_LOCK_ACQUISITION_FAILED);
		}
	}

	public void unlockAll(List<RLock> locks) {
		for (int i = locks.size() - 1; i >= 0; i--) {
			RLock lock = locks.get(i);
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	private String buildKey(Long matchId, Long seatId) {
		return KEY_PREFIX + matchId + ":seat:" + seatId;
	}
}