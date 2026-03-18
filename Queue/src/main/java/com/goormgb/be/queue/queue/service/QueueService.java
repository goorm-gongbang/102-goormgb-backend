package com.goormgb.be.queue.queue.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.model.SeatPreferenceCache;
import com.goormgb.be.queue.config.QueueProperties;
import com.goormgb.be.queue.queue.dto.request.QueueEnterRequest;
import com.goormgb.be.queue.queue.dto.response.QueueEnterResponse;
import com.goormgb.be.queue.queue.dto.response.QueueStatusResponse;
import com.goormgb.be.queue.queue.model.ReadyTokenPayload;
import com.goormgb.be.queue.queue.policy.QueuePollingPolicy;
import com.goormgb.be.queue.queue.repository.QueueRedisRepository;

import io.micrometer.core.instrument.Counter;
@Service
public class QueueService {

	private final MatchRepository matchRepository;
	private final QueueRedisRepository queueRedisRepository;
	private final QueueProperties queueProperties;
	private final QueuePollingPolicy queuePollingPolicy;
	private final Counter queueEntriesCounter;

	public QueueService(
		MatchRepository matchRepository,
		QueueRedisRepository queueRedisRepository,
		QueueProperties queueProperties,
		QueuePollingPolicy queuePollingPolicy,
		@Qualifier("queueEntriesCounter") Counter queueEntriesCounter
	) {
		this.matchRepository = matchRepository;
		this.queueRedisRepository = queueRedisRepository;
		this.queueProperties = queueProperties;
		this.queuePollingPolicy = queuePollingPolicy;
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

	@Transactional
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

	private void validateQueueOpen(Match match) {
		if (match.getSaleStatus() != SaleStatus.ON_SALE) {
			throw new CustomException(ErrorCode.MATCH_NOT_AVAILABLE_FOR_QUEUE);
		}
	}

	private void validateEnterRequest(QueueEnterRequest request) {
		if (request.ticketCount() < 1 || request.ticketCount() > 10) {
			throw new CustomException(ErrorCode.INVALID_TICKET_COUNT);
		}
	}

	private void requireAuthenticated(Long userId) {
		if (userId == null || SecurityContextHolder.getContext().getAuthentication() == null) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}
}
