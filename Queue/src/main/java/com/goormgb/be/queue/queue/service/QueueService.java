package com.goormgb.be.queue.queue.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.queue.config.QueueProperties;
import com.goormgb.be.queue.queue.dto.request.QueueEnterRequest;
import com.goormgb.be.queue.queue.dto.response.QueueEnterResponse;
import com.goormgb.be.queue.queue.dto.response.QueueStatusResponse;
import com.goormgb.be.queue.queue.model.ReadyTokenPayload;
import com.goormgb.be.queue.queue.model.SeatPreferenceCache;
import com.goormgb.be.queue.queue.policy.QueuePollingPolicy;
import com.goormgb.be.queue.queue.repository.QueueRedisRepository;
import com.goormgb.be.queue.queue.security.AdmissionTokenProvider;

import io.micrometer.core.instrument.Counter;
@Service
public class QueueService {

	private final MatchRepository matchRepository;
	private final QueueRedisRepository queueRedisRepository;
	private final QueueProperties queueProperties;
	private final QueuePollingPolicy queuePollingPolicy;
	private final AdmissionTokenProvider admissionTokenProvider;
	private final Counter queueEntriesCounter;

	public QueueService(
		MatchRepository matchRepository,
		QueueRedisRepository queueRedisRepository,
		QueueProperties queueProperties,
		QueuePollingPolicy queuePollingPolicy,
		AdmissionTokenProvider admissionTokenProvider,
		@Qualifier("queueEntriesCounter") Counter queueEntriesCounter
	) {
		this.matchRepository = matchRepository;
		this.queueRedisRepository = queueRedisRepository;
		this.queueProperties = queueProperties;
		this.queuePollingPolicy = queuePollingPolicy;
		this.admissionTokenProvider = admissionTokenProvider;
		this.queueEntriesCounter = queueEntriesCounter;
	}

	@Transactional
	public QueueEnterResponse enter(Long matchId, Long userId, QueueEnterRequest request) {
		requireAuthenticated(userId);

		Match match = matchRepository.findByIdOrThrow(matchId, ErrorCode.MATCH_NOT_FOUND);
		validateQueueOpen(match);
		validateEnterRequest(request);

		if (queueRedisRepository.isAlreadyQueued(matchId, userId)) {
			throw new CustomException(ErrorCode.QUEUE_ALREADY_ENTERED);
		}

		// 재진입 성공 시 이전 EXPIRED 흔적은 제거한다.
		queueRedisRepository.deleteExpiredMarker(matchId, userId);

		SeatPreferenceCache preference = new SeatPreferenceCache(
			userId,
			matchId,
			request.recommendationEnabled(),
			request.ticketCount(),
			normalizePreferredBlockIds(request.preferredBlockIds()),
			Instant.now()
		);

		queueRedisRepository.saveSeatPreference(
			preference,
			Duration.ofSeconds(queueProperties.preferenceTtlSeconds())
		);
		queueRedisRepository.addToWaitingQueue(matchId, userId, Instant.now().toEpochMilli());
		queueRedisRepository.addActiveMatch(matchId);

		queueEntriesCounter.increment();

		return QueueEnterResponse.waiting(
			queueRedisRepository.getWaitingRank(matchId, userId),
			queueRedisRepository.getWaitingCount(matchId)
		);
	}

	@Transactional(readOnly = true)
	public QueueStatusResponse getStatus(Long matchId, Long userId) {
		requireAuthenticated(userId);

		ReadyTokenPayload readyTokenPayload = queueRedisRepository.getReadyToken(matchId, userId);
		if (readyTokenPayload != null) {
			long ttlSeconds = queueRedisRepository.getReadyTokenTtlSeconds(matchId, userId);
			if (ttlSeconds > 0) {
				return QueueStatusResponse.ready(readyTokenPayload.admissionToken(), ttlSeconds);
			}

			queueRedisRepository.markExpired(
				matchId,
				userId,
				Duration.ofSeconds(queueProperties.expiredMarkerTtlSeconds())
			);
		}

		if (queueRedisRepository.isExpired(matchId, userId)) {
			throw new CustomException(ErrorCode.ADMISSION_TOKEN_EXPIRED);
		}

		long rank = queueRedisRepository.getWaitingRank(matchId, userId);
		if (rank <= 0) {
			throw new CustomException(ErrorCode.QUEUE_ENTRY_NOT_FOUND);
		}

		return QueueStatusResponse.waiting(
			rank,
			queueRedisRepository.getWaitingCount(matchId),
			queuePollingPolicy.forWaiting(rank)
		);
	}

	@Transactional
	public void promoteActiveMatches() {
		for (Long matchId : queueRedisRepository.getActiveMatches()) {
			promoteWaitingUsers(matchId);
		}
	}

	@Transactional
	public int promoteWaitingUsers(Long matchId) {
		int activeReadyCount = syncExpiredReadyUsers(matchId);
		int permit = Math.max(0, queueProperties.promotionBatchSize() - activeReadyCount);
		if (permit == 0) {
			return 0;
		}

		List<Long> promotedUserIds = queueRedisRepository.popWaitingUsers(matchId, permit);
		if (promotedUserIds.isEmpty()) {
			cleanupInactiveMatch(matchId, activeReadyCount);
			return 0;
		}

		Duration readyTtl = Duration.ofSeconds(queueProperties.readyTtlSeconds());
		for (Long userId : promotedUserIds) {
			String token = admissionTokenProvider.issue(userId, matchId, readyTtl);
			ReadyTokenPayload payload = new ReadyTokenPayload(
				userId,
				matchId,
				token,
				Instant.now(),
				Instant.now().plusSeconds(queueProperties.readyTtlSeconds())
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

	private void validateQueueOpen(Match match) {
		if (match.getSaleStatus() != SaleStatus.ON_SALE) {
			throw new CustomException(ErrorCode.MATCH_NOT_AVAILABLE_FOR_QUEUE);
		}
	}

	private void validateEnterRequest(QueueEnterRequest request) {
		if (request.ticketCount() < 1 || request.ticketCount() > 10) {
			throw new CustomException(ErrorCode.INVALID_TICKET_COUNT);
		}

		List<Long> blockIds = normalizePreferredBlockIds(request.preferredBlockIds());
		if (blockIds.size() != blockIds.stream().distinct().count()) {
			throw new CustomException(ErrorCode.DUPLICATE_PREFERRED_BLOCK);
		}
	}

	private List<Long> normalizePreferredBlockIds(List<Long> preferredBlockIds) {
		return preferredBlockIds == null ? List.of() : preferredBlockIds;
	}

	private void requireAuthenticated(Long userId) {
		if (userId == null || SecurityContextHolder.getContext().getAuthentication() == null) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}
}
