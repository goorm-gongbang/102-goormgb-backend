package com.goormgb.be.seat.matchSeat.repository;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

/**
 * 사용자별 경기당 구매 완료 좌석 수를 Redis에 캐싱하는 Repository.
 *
 * <p>Kafka 이벤트(결제완료/주문취소)를 통해 카운터를 동기화하며,
 * Seat 서비스의 Hold 단계에서 최대 예매 수량(8매) 초과 여부를 사전 검증할 때 사용된다.</p>
 *
 * <h3>Redis Key</h3>
 * <pre>purchased:{userId}:{matchId}</pre>
 *
 * <h3>TTL</h3>
 * <p>90일 — 경기 종료 후 자동 만료</p>
 *
 * <h3>일관성 보장 수준</h3>
 * <p>Kafka at-least-once 특성으로 중복 처리될 수 있으나,
 * Order-Core의 DB 기반 검증이 최종 방어선이므로 허용 가능한 수준의 오차.</p>
 * <p>Redis 장애 시 {@code get()}은 0을 반환하여 fail-open 처리한다.</p>
 */
@Slf4j
@Repository
public class UserPurchasedSeatCountRepository {

	private static final String KEY_PREFIX = "purchased:";
	private static final Duration TTL = Duration.ofDays(90);

	private final StringRedisTemplate stringRedisTemplate;

	public UserPurchasedSeatCountRepository(
			@Qualifier("stringRedisTemplate") StringRedisTemplate stringRedisTemplate
	) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	/**
	 * 해당 유저의 해당 경기 구매 완료 좌석 수를 반환한다.
	 *
	 * <p>키가 없거나 Redis 장애 시 0L을 반환한다.</p>
	 *
	 * @param userId  사용자 ID
	 * @param matchId 경기 ID
	 * @return 구매 완료 좌석 수 (0 이상)
	 */
	public long get(Long userId, Long matchId) {
		try {
			String value = stringRedisTemplate.opsForValue().get(key(userId, matchId));
			return value == null ? 0L : Long.parseLong(value);
		} catch (Exception e) {
			log.warn("[PurchasedSeatCount 경기 구매 완료 좌석 수] Redis 조회 실패 — fail-open으로 0 반환: userId={}, matchId={}",
					userId, matchId, e);
			return 0L;
		}
	}

	/**
	 * 구매 완료 좌석 수를 증가시킨다.
	 *
	 * <p>결제 완료(PaymentCompletedEvent) 시 호출한다.</p>
	 *
	 * @param userId  사용자 ID
	 * @param matchId 경기 ID
	 * @param count   증가할 좌석 수
	 */
	public void increment(Long userId, Long matchId, int count) {
		try {
			String key = key(userId, matchId);
			stringRedisTemplate.opsForValue().increment(key, count);
			stringRedisTemplate.expire(key, TTL);
			log.debug("[PurchasedSeatCount 경기 구매 완료 좌석 수] 증가: userId={}, matchId={}, count={}", userId, matchId, count);
		} catch (Exception e) {
			log.warn("[PurchasedSeatCount 경기 구매 완료 좌석 수] Redis 증가 실패: userId={}, matchId={}, count={}", userId, matchId,
					count, e);
		}
	}

	/**
	 * 구매 완료 좌석 수를 감소시킨다.
	 *
	 * <p>주문 취소(OrderCancelledEvent) 시 호출한다.
	 * 감소 결과가 음수가 되면 0으로 보정한다.</p>
	 *
	 * @param userId  사용자 ID
	 * @param matchId 경기 ID
	 * @param count   감소할 좌석 수
	 */
	public void decrement(Long userId, Long matchId, int count) {
		try {
			String key = key(userId, matchId);
			Long result = stringRedisTemplate.opsForValue().decrement(key, count);
			if (result != null && result < 0) {
				stringRedisTemplate.opsForValue().set(key, "0", TTL);
				log.debug("[PurchasedSeatCount 경기 구매 완료 좌석 수] 감소 후 음수 보정 → 0: userId={}, matchId={}", userId, matchId);
			} else if (result != null) {
				stringRedisTemplate.expire(key, TTL);
				log.debug("[PurchasedSeatCount 경기 구매 완료 좌석 수] 감소: userId={}, matchId={}, count={}, result={}", userId,
						matchId, count,
						result);
			}
		} catch (Exception e) {
			log.warn("[PurchasedSeatCount 경기 구매 완료 좌석 수] Redis 감소 실패: userId={}, matchId={}, count={}", userId, matchId,
					count, e);
		}
	}

	private String key(Long userId, Long matchId) {
		return KEY_PREFIX + userId + ":" + matchId;
	}
}
