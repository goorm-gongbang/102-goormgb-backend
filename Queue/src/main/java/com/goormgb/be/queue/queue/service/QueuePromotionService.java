package com.goormgb.be.queue.queue.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.queue.config.QueueProperties;
import com.goormgb.be.queue.queue.model.ReadyTokenPayload;
import com.goormgb.be.queue.queue.repository.QueueRedisRepository;
import com.goormgb.be.queue.queue.security.AdmissionTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueuePromotionService {

	private final QueueRedisRepository queueRedisRepository;
	private final QueueProperties queueProperties;
	private final AdmissionTokenProvider admissionTokenProvider;

	public void promoteActiveMatches() {
		for (Long matchId : queueRedisRepository.getActiveMatches()) {
			try {
				promoteWaitingUsers(matchId);
			} catch (Exception e) {
				log.error("Queue promotion failed for matchId={}", matchId, e);
			}
		}
	}

	@Transactional
	public int promoteWaitingUsers(Long matchId) {
		int activeReadyCount = syncExpiredReadyUsers(matchId);
		int permit = Math.max(0, queueProperties.promotionBatchSize() - activeReadyCount);
		if (permit == 0) {
			cleanupInactiveMatch(matchId, activeReadyCount);
			return 0;
		}

		List<Long> promotedUserIds = queueRedisRepository.popWaitingUsers(matchId, permit);
		if (promotedUserIds.isEmpty()) {
			cleanupInactiveMatch(matchId, activeReadyCount);
			return 0;
		}

		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plusSeconds(queueProperties.readyTtlSeconds());
		Duration readyTtl = Duration.ofSeconds(queueProperties.readyTtlSeconds());

		for (Long userId : promotedUserIds) {
			String token = admissionTokenProvider.issue(userId, matchId, readyTtl);
			ReadyTokenPayload payload = new ReadyTokenPayload(
				userId,
				matchId,
				token,
				issuedAt,
				expiresAt
			);
			queueRedisRepository.saveReadyToken(payload);
		}

		cleanupInactiveMatch(matchId, activeReadyCount + promotedUserIds.size());
		return promotedUserIds.size();
	}

	// READY 인덱스는 Redis TTL과 별도로 남을 수 있어, 스케줄러가 stale entry를 정리해준다.
	private int syncExpiredReadyUsers(Long matchId) {
		Set<Long> readyUserIds = queueRedisRepository.getReadyUserIds(matchId);
		if (readyUserIds.isEmpty()) {
			return 0;
		}

		int activeReadyCount = 0;
		for (Long userId : readyUserIds) {
			if (queueRedisRepository.hasReadyToken(matchId, userId)) {
				activeReadyCount++;
				continue;
			}

			queueRedisRepository.markExpired(
				matchId,
				userId,
				Duration.ofSeconds(queueProperties.expiredMarkerTtlSeconds())
			);
		}

		return activeReadyCount;
	}

	private void cleanupInactiveMatch(Long matchId, int activeReadyCount) {
		if (queueRedisRepository.getWaitingCount(matchId) == 0 && activeReadyCount == 0) {
			queueRedisRepository.removeActiveMatch(matchId);
		}
	}
}
