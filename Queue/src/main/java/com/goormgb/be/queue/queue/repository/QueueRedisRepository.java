package com.goormgb.be.queue.queue.repository;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormgb.be.queue.config.QueueProperties;
import com.goormgb.be.queue.queue.model.ReadyTokenPayload;
import com.goormgb.be.queue.queue.model.WaitingQueueEntry;

@Repository
public class QueueRedisRepository {

	private final StringRedisTemplate redisTemplate;
	private final QueueProperties queueProperties;
	private final ObjectMapper redisObjectMapper;

	public QueueRedisRepository(
		StringRedisTemplate redisTemplate,
		QueueProperties queueProperties,
		@Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper
	) {
		this.redisTemplate = redisTemplate;
		this.queueProperties = queueProperties;
		this.redisObjectMapper = redisObjectMapper;
	}

	public void addToWaitingQueue(Long matchId, Long userId, long enteredAtMillis) {
		redisTemplate.opsForZSet().add(queueProperties.waitKey(matchId), String.valueOf(userId), enteredAtMillis);
	}

	public boolean isUserInWaitingQueue(Long matchId, Long userId) {
		return redisTemplate.opsForZSet().score(queueProperties.waitKey(matchId), String.valueOf(userId)) != null;
	}

	public long getWaitingRank(Long matchId, Long userId) {
		Long rank = redisTemplate.opsForZSet().rank(queueProperties.waitKey(matchId), String.valueOf(userId));
		if (rank == null) {
			return -1L;
		}
		return rank + 1;
	}

	public long getWaitingCount(Long matchId) {
		Long count = redisTemplate.opsForZSet().zCard(queueProperties.waitKey(matchId));
		return count == null ? 0L : count;
	}

	public List<WaitingQueueEntry> popWaitingUsers(Long matchId, long count) {
		Set<ZSetOperations.TypedTuple<String>> entries =
			redisTemplate.opsForZSet().popMin(queueProperties.waitKey(matchId), count);

		if (entries == null || entries.isEmpty()) {
			return List.of();
		}

		return entries.stream()
			.filter(tuple -> tuple.getValue() != null && tuple.getScore() != null)
			.map(tuple -> WaitingQueueEntry.of(
				Long.valueOf(tuple.getValue()),
				tuple.getScore().longValue()
			))
			.toList();
	}

	public void removeFromWaitingQueue(Long matchId, Long userId) {
		redisTemplate.opsForZSet().remove(queueProperties.waitKey(matchId), String.valueOf(userId));
	}

	public void saveReadyToken(ReadyTokenPayload payload, Duration ttl) {
		setJson(
			queueProperties.readyKey(payload.matchId(), payload.userId()),
			payload,
			ttl
		);
		addReadyIndex(payload.matchId(), payload.userId());
	}

	public ReadyTokenPayload getReadyToken(Long matchId, Long userId) {
		return getJson(queueProperties.readyKey(matchId, userId), ReadyTokenPayload.class);
	}

	public boolean hasReadyToken(Long matchId, Long userId) {
		Boolean exists = redisTemplate.hasKey(queueProperties.readyKey(matchId, userId));
		return Boolean.TRUE.equals(exists);
	}

	public boolean isAlreadyQueued(Long matchId, Long userId) {
		return isUserInWaitingQueue(matchId, userId) || hasReadyToken(matchId, userId);
	}

	public long getReadyTokenTtlSeconds(Long matchId, Long userId) {
		Long ttlSeconds = redisTemplate.getExpire(queueProperties.readyKey(matchId, userId));
		if (ttlSeconds == null) {
			return -2L;
		}
		return ttlSeconds;
	}

	public void deleteReadyToken(Long matchId, Long userId) {
		redisTemplate.delete(queueProperties.readyKey(matchId, userId));
		removeReadyIndex(matchId, userId);
	}

	public void addActiveMatch(Long matchId) {
		redisTemplate.opsForSet().add(queueProperties.activeMatchKey(), String.valueOf(matchId));
	}

	public void removeActiveMatch(Long matchId) {
		redisTemplate.opsForSet().remove(queueProperties.activeMatchKey(), String.valueOf(matchId));
	}

	public Set<Long> getActiveMatches() {
		Set<String> matches = redisTemplate.opsForSet().members(queueProperties.activeMatchKey());
		if (matches == null || matches.isEmpty()) {
			return Set.of();
		}

		return matches.stream()
			.map(Long::valueOf)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	public void markExpired(Long matchId, Long userId, Duration ttl) {
		redisTemplate.opsForValue().set(queueProperties.expiredKey(matchId, userId), "1", ttl);
		removeReadyIndex(matchId, userId);
	}

	public boolean isExpired(Long matchId, Long userId) {
		Boolean exists = redisTemplate.hasKey(queueProperties.expiredKey(matchId, userId));
		return Boolean.TRUE.equals(exists);
	}

	public void deleteExpiredMarker(Long matchId, Long userId) {
		redisTemplate.delete(queueProperties.expiredKey(matchId, userId));
	}

	public void addReadyIndex(Long matchId, Long userId) {
		redisTemplate.opsForSet().add(queueProperties.readyIndexKey(matchId), String.valueOf(userId));
	}

	public void removeReadyIndex(Long matchId, Long userId) {
		redisTemplate.opsForSet().remove(queueProperties.readyIndexKey(matchId), String.valueOf(userId));
	}

	public Set<Long> getReadyUserIds(Long matchId) {
		Set<String> userIds = redisTemplate.opsForSet().members(queueProperties.readyIndexKey(matchId));
		if (userIds == null || userIds.isEmpty()) {
			return Set.of();
		}

		return userIds.stream()
			.map(Long::valueOf)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	private void setJson(String key, Object value) {
		try {
			redisTemplate.opsForValue().set(key, redisObjectMapper.writeValueAsString(value));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize Redis value for key: " + key, e);
		}
	}

	private void setJson(String key, Object value, Duration ttl) {
		try {
			redisTemplate.opsForValue().set(key, redisObjectMapper.writeValueAsString(value), ttl);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize Redis value for key: " + key, e);
		}
	}

	private <T> T getJson(String key, Class<T> type) {
		String raw = redisTemplate.opsForValue().get(key);
		if (raw == null) {
			return null;
		}

		try {
			return redisObjectMapper.readValue(raw, type);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to deserialize Redis value for key: " + key, e);
		}
	}

	public void leaveQueueAtomic(Long matchId, Long userId) {
		String script =
			// 1. 대기열(ZSET)에서 유저 삭제
			"redis.call('ZREM', KEYS[1], ARGV[1]); " +
				// 2. READY 토큰(String) 삭제
				"redis.call('DEL', KEYS[2]); " +
				// 3. 만료 마커(String) 삭제
				"redis.call('DEL', KEYS[3]); " +
				// 4. READY 인덱스(SET)에서 유저 삭제
				"redis.call('SREM', KEYS[4], ARGV[1]); " +
				// 5. 대기열과 READY 인덱스가 모두 비었는지 확인 (ZCARD, SCARD 사용)
				"if redis.call('ZCARD', KEYS[1]) == 0 and redis.call('SCARD', KEYS[4]) == 0 then " +
				// 활성 경기 목록에서 제거
				"  redis.call('SREM', KEYS[5], ARGV[2]); " + "end";

		List<String> keys = List.of(
			queueProperties.waitKey(matchId),      // KEYS[1]
			queueProperties.readyKey(matchId, userId), // KEYS[2]
			queueProperties.expiredKey(matchId, userId), // KEYS[3]
			queueProperties.readyIndexKey(matchId), // KEYS[4]
			queueProperties.activeMatchKey()        // KEYS[5]
		);

		redisTemplate.execute(
			new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Void.class),
			keys,
			String.valueOf(userId), // ARGV[1]
			String.valueOf(matchId) // ARGV[2]
		);
	}

	/**
	 * 재진입 처리와 대기열 순번·총원 조회를 단일 Lua 스크립트로 통합 수행한다 (Phase 4).
	 *
	 * <p>기존 구조는 {@code reenterQueueAtomic} (Lua) → {@code getWaitingRank} (ZRANK) →
	 * {@code getWaitingCount} (ZCARD) 3회의 Redis 왕복으로 이루어져 있었다. 진입과 조회에
	 * 같은 key 를 사용하므로 write 뒤 read 를 분리할 이유가 없어, Lua 스크립트 말미에
	 * ZRANK + ZCARD 를 추가해 {@code {rank, count}} 로 한 번에 반환한다.</p>
	 *
	 * <p>반환값 규약:
	 * <ul>
	 *   <li>{@code [0]} — 0-based ZRANK. 호출자가 필요 시 1-based 로 보정한다
	 *       (기존 {@link #getWaitingRank(Long, Long)} 는 +1 보정 후 반환했다).</li>
	 *   <li>{@code [1]} — ZCARD 값 (총 대기자 수).</li>
	 * </ul>
	 *
	 * <p>삭제·재등록·조회의 원자성을 유지하므로 중간 상태 노출이나 rank/count 불일치가 발생하지 않는다.</p>
	 *
	 * @return {@code [rank, count]} List. ZRANK 결과가 없으면 [0] 은 null 일 수 있으나
	 *         직전에 ZADD 를 수행했으므로 실제로는 항상 유효한 값이 반환된다.
	 */
	public List<Long> reenterQueueAtomicWithRankCount(Long matchId, Long userId, long enteredAtMillis) {
		String script =
			// 1. 기존 WAITING 순번 제거
			"redis.call('ZREM', KEYS[1], ARGV[1]); " +
				// 2. 기존 READY admission token 제거
				"redis.call('DEL', KEYS[2]); " +
				// 3. 만료 마커 제거
				"redis.call('DEL', KEYS[3]); " +
				// 4. READY 인덱스에서 사용자 제거
				"redis.call('SREM', KEYS[4], ARGV[1]); " +
				// 5. 새 진입 시각으로 대기열 맨 뒤에 다시 등록
				"redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1]); " +
				// 6. 활성 경기 목록에 현재 경기 보장
				"redis.call('SADD', KEYS[5], ARGV[3]); " +
				// 7. 현재 사용자 0-based rank 조회 (직전 ZADD 뒤라 항상 존재)
				"local rank = redis.call('ZRANK', KEYS[1], ARGV[1]); " +
				// 8. 현재 대기열 총원 조회
				"local count = redis.call('ZCARD', KEYS[1]); " +
				"return {rank, count};";

		List<String> keys = List.of(
			queueProperties.waitKey(matchId),
			queueProperties.readyKey(matchId, userId),
			queueProperties.expiredKey(matchId, userId),
			queueProperties.readyIndexKey(matchId),
			queueProperties.activeMatchKey()
		);

		@SuppressWarnings("unchecked")
		List<Long> result = redisTemplate.execute(
			new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, List.class),
			keys,
			String.valueOf(userId),
			String.valueOf(enteredAtMillis),
			String.valueOf(matchId)
		);

		return result;
	}

}
