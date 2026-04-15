package com.goormgb.be.queue.queue.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.support.SalesOpenUtils;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.queue.config.QueueProperties;
import com.goormgb.be.queue.metrics.QueueMetricsService;
import com.goormgb.be.queue.queue.dto.response.QueueEnterResponse;
import com.goormgb.be.queue.queue.dto.response.QueueStatusResponse;
import com.goormgb.be.queue.queue.model.ReadyTokenPayload;
import com.goormgb.be.queue.queue.policy.QueuePollingPolicy;
import com.goormgb.be.queue.queue.repository.QueueRedisRepository;

@Service
public class QueueService {

	private final MatchQueueCacheService matchQueueCacheService;
	private final QueueRedisRepository queueRedisRepository;
	private final QueueProperties queueProperties;
	private final QueuePollingPolicy queuePollingPolicy;
	private final QueueMetricsService queueMetricsService;
	private final PreQueueValidationService preQueueValidationService;
	private final SalesOpenUtils salesOpenUtils;

	public QueueService(
		MatchQueueCacheService matchQueueCacheService,
		QueueRedisRepository queueRedisRepository,
		QueueProperties queueProperties,
		QueuePollingPolicy queuePollingPolicy,
		QueueMetricsService queueMetricsService,
		PreQueueValidationService preQueueValidationService,
		SalesOpenUtils salesOpenUtils
	) {
		this.matchQueueCacheService = matchQueueCacheService;
		this.queueRedisRepository = queueRedisRepository;
		this.queueProperties = queueProperties;
		this.queuePollingPolicy = queuePollingPolicy;
		this.queueMetricsService = queueMetricsService;
		this.preQueueValidationService = preQueueValidationService;
		this.salesOpenUtils = salesOpenUtils;
	}

	@Transactional
	public QueueEnterResponse enter(Long matchId, Long userId) {
		requireAuthenticated(userId);
		preQueueValidationService.validateBeforeEnter(matchId, userId);

		Match match = matchQueueCacheService.getForQueue(matchId);
		validateQueueOpen(match);

		long enteredAtMillis = Instant.now().toEpochMilli();

		// Phase 4 — 재진입 처리 + ZRANK + ZCARD 를 단일 Lua 스크립트로 통합 (Redis 왕복 3 → 1)
		List<Long> rankAndCount = queueRedisRepository
			.reenterQueueAtomicWithRankCount(matchId, userId, enteredAtMillis);
		// ZRANK 는 0-based 이므로 기존 getWaitingRank 동작(+1) 과 일치시키기 위해 보정
		long rank = rankAndCount.get(0) + 1;
		long count = rankAndCount.get(1);

		// 대기열 진입 건수 집계
		queueMetricsService.recordEntry();

		return QueueEnterResponse.waiting(rank, count);
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

		Preconditions.validate(!queueRedisRepository.isExpired(matchId, userId),
			ErrorCode.ADMISSION_TOKEN_EXPIRED);

		long rank = queueRedisRepository.getWaitingRank(matchId, userId);
		Preconditions.validate(rank > 0, ErrorCode.QUEUE_ENTRY_NOT_FOUND);

		return QueueStatusResponse.waiting(
			rank,
			queueRedisRepository.getWaitingCount(matchId),
			queuePollingPolicy.forWaiting(rank)
		);
	}

	public void leave(Long matchId, Long userId) {
		requireAuthenticated(userId);

		/*queueRedisRepository.removeFromWaitingQueue(matchId, userId);
		queueRedisRepository.deleteReadyToken(matchId, userId);
		queueRedisRepository.deleteExpiredMarker(matchId, userId);

		if (queueRedisRepository.getWaitingCount(matchId) == 0
			&& queueRedisRepository.getReadyUserIds(matchId).isEmpty()) {
			queueRedisRepository.removeActiveMatch(matchId);
		}*/
		// 개별 호출 대신 원자적 스크립트 실행
		queueRedisRepository.leaveQueueAtomic(matchId, userId);
		// 대기열 이탈 건수 집계
		queueMetricsService.recordAbandoned();
	}

	/**
	 * 대기열 진입 가능 여부를 판정한다.
	 *
	 * <p>판정은 {@link SalesOpenUtils#isPurchasable(Match, Instant)} 로 위임하여
	 * Order-Core 의 화면 표기 로직과 동일 기준(시간 기반 Lazy) 으로 운영된다.</p>
	 *
	 * <p>기존에는 DB {@code sale_status == ON_SALE} 단일 조건으로 판정했으나,
	 * 상태 전환 스케줄러의 트랜잭션 커밋 지연과 로컬 캐시 TTL 로 인해 11시 정각
	 * 오픈 시점 유저가 최대 1~2분간 {@code 409 MATCH_NOT_AVAILABLE_FOR_QUEUE} 를 겪는
	 * 문제가 있었다. 시간 기반(Lazy) 판정으로 전환하여 스케줄러·캐시 지연과 무관하게
	 * 11:00:00 시점 진입이 허용된다.</p>
	 */
	private void validateQueueOpen(Match match) {
		Preconditions.validate(salesOpenUtils.isPurchasable(match, Instant.now()),
			ErrorCode.MATCH_NOT_AVAILABLE_FOR_QUEUE);
	}

	private void requireAuthenticated(Long userId) {
		Preconditions.validate(
			userId != null && SecurityContextHolder.getContext().getAuthentication() != null,
			ErrorCode.UNAUTHORIZED);
	}
}
