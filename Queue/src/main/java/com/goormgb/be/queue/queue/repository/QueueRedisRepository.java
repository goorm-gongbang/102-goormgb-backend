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
import com.goormgb.be.global.model.SeatPreferenceCache;
import com.goormgb.be.queue.config.QueueProperties;
import com.goormgb.be.queue.queue.model.ReadyTokenPayload;

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

	public void saveSeatPreference(SeatPreferenceCache preference) {
		setJson(
			queueProperties.preferenceKey(preference.matchId(), preference.userId()),
			preference
		);
	}

	public void saveSeatPreference(SeatPreferenceCache preference, Duration ttl) {
		setJson(
			queueProperties.preferenceKey(preference.matchId(), preference.userId()),
			preference,
			ttl
		);
	}

	public SeatPreferenceCache getSeatPreference(Long matchId, Long userId) {
		return getJson(queueProperties.preferenceKey(matchId, userId), SeatPreferenceCache.class);
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

	public List<Long> popWaitingUsers(Long matchId, long count) {
		Set<ZSetOperations.TypedTuple<String>> entries =
			redisTemplate.opsForZSet().popMin(queueProperties.waitKey(matchId), count);

		if (entries == null || entries.isEmpty()) {
			return List.of();
		}

		return entries.stream()
			.map(ZSetOperations.TypedTuple::getValue)
			.map(Long::valueOf)
			.toList();
	}

	public void removeFromWaitingQueue(Long matchId, Long userId) {
		redisTemplate.opsForZSet().remove(queueProperties.waitKey(matchId), String.valueOf(userId));
	}

	public void saveReadyToken(ReadyTokenPayload payload) {
		setJson(
			queueProperties.readyKey(payload.matchId(), payload.userId()),
			payload,
			Duration.ofSeconds(queueProperties.readyTtlSeconds())
		);
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
	}

	public boolean isExpired(Long matchId, Long userId) {
		Boolean exists = redisTemplate.hasKey(queueProperties.expiredKey(matchId, userId));
		return Boolean.TRUE.equals(exists);
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
}
