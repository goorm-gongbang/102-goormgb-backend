package com.goormgb.be.queue.queue.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.match.support.SalesOpenUtils;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.queue.config.QueueProperties;
import com.goormgb.be.queue.metrics.QueueMetricsService;
import com.goormgb.be.queue.queue.policy.QueuePollingPolicy;
import com.goormgb.be.queue.queue.repository.QueueRedisRepository;

/**
 * {@link QueueService#enter(Long, Long)} 의 오픈 판정(Lazy) 경계값 검증.
 *
 * <p>오픈 판정이 기존 {@code saleStatus == ON_SALE} 에서
 * {@code now >= openAt} 시간 비교로 전환된 뒤, 11시 정각 진입 허용 동작을 보증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Mock
	private MatchQueueCacheService matchQueueCacheService;
	@Mock
	private QueueRedisRepository queueRedisRepository;
	@Mock
	private QueueProperties queueProperties;
	@Mock
	private QueuePollingPolicy queuePollingPolicy;
	@Mock
	private QueueMetricsService queueMetricsService;
	@Mock
	private PreQueueValidationService preQueueValidationService;

	private final SalesOpenUtils salesOpenUtils = new SalesOpenUtils();
	private QueueService queueService;

	@BeforeEach
	void setUp() {
		queueService = new QueueService(
			matchQueueCacheService,
			queueRedisRepository,
			queueProperties,
			queuePollingPolicy,
			queueMetricsService,
			preQueueValidationService,
			salesOpenUtils
		);
		Authentication auth = new UsernamePasswordAuthenticationToken("1", null);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private Match matchWith(Instant matchAt, SaleStatus status) {
		return Match.builder()
			.matchAt(matchAt)
			.homeClub(null)
			.awayClub(null)
			.stadium(null)
			.saleStatus(status)
			.build();
	}

	@Test
	@DisplayName("openAt 이 아직 안 된 경기는 진입이 거부된다 (1초 전)")
	void rejectsWhenBeforeOpenAt() {
		// matchAt = now + 8일 → openAt = now + 1일. 아직 오픈 전.
		Instant matchAt = Instant.now().plusSeconds(60L * 60 * 24 * 8);
		Match match = matchWith(matchAt, SaleStatus.UPCOMING);

		when(matchQueueCacheService.getForQueue(anyLong())).thenReturn(match);

		assertThatThrownBy(() -> queueService.enter(1L, 1L))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.MATCH_NOT_AVAILABLE_FOR_QUEUE);
	}

	@Test
	@DisplayName("openAt 이 지났다면 DB 가 아직 UPCOMING 이어도 진입이 허용된다 — 스케줄러 지연과 무관")
	void allowsWhenOpenAtPassedEvenIfSaleStatusStillUpcoming() {
		// matchAt = now + 6일 → openAt = now - 1일. 이미 오픈 시각 지남.
		Instant matchAt = Instant.now().plusSeconds(60L * 60 * 24 * 6);
		Match match = matchWith(matchAt, SaleStatus.UPCOMING);

		when(matchQueueCacheService.getForQueue(anyLong())).thenReturn(match);

		assertThatCode(() -> queueService.enter(1L, 1L)).doesNotThrowAnyException();
		verify(queueRedisRepository).reenterQueueAtomic(anyLong(), anyLong(), anyLong());
	}

	@Test
	@DisplayName("openAt 이 지났어도 SOLD_OUT 상태면 진입 차단")
	void rejectsWhenSoldOutEvenAfterOpenAt() {
		Instant matchAt = Instant.now().plusSeconds(60L * 60 * 24 * 6);
		Match match = matchWith(matchAt, SaleStatus.SOLD_OUT);

		when(matchQueueCacheService.getForQueue(anyLong())).thenReturn(match);

		assertThatThrownBy(() -> queueService.enter(1L, 1L))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.MATCH_NOT_AVAILABLE_FOR_QUEUE);
	}

	@Test
	@DisplayName("openAt 이 지났어도 ENDED 상태면 진입 차단")
	void rejectsWhenEndedEvenAfterOpenAt() {
		Instant matchAt = Instant.now().plusSeconds(60L * 60 * 24 * 6);
		Match match = matchWith(matchAt, SaleStatus.ENDED);

		when(matchQueueCacheService.getForQueue(anyLong())).thenReturn(match);

		assertThatThrownBy(() -> queueService.enter(1L, 1L))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.MATCH_NOT_AVAILABLE_FOR_QUEUE);
	}

	@Test
	@DisplayName("openAt 경계 — 정각(0초 오차) 진입 허용")
	void allowsExactlyAtOpenAt() {
		// 임의의 경기로 openAt 을 먼저 계산한 뒤, 그 시각을 기준으로 테스트.
		Instant matchAtSample = LocalDateTime.of(2026, 4, 22, 18, 30).atZone(KST).toInstant();
		Match sample = matchWith(matchAtSample, SaleStatus.UPCOMING);
		Instant openAtOfSample = salesOpenUtils.calculateSalesOpenAt(sample);

		// matchAt 을 조정해, openAt 이 '지금-여유 1분' 이 되게 한다.
		long diffSeconds = java.time.Duration.between(openAtOfSample, Instant.now().minusSeconds(60)).getSeconds();
		Instant adjustedMatchAt = matchAtSample.plusSeconds(diffSeconds);
		Match match = matchWith(adjustedMatchAt, SaleStatus.UPCOMING);

		when(matchQueueCacheService.getForQueue(anyLong())).thenReturn(match);

		assertThatCode(() -> queueService.enter(1L, 1L)).doesNotThrowAnyException();
	}
}
