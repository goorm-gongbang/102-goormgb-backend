package com.goormgb.be.ordercore.match.scheduler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.ordercore.match.entity.Match;
import com.goormgb.be.ordercore.match.enums.SaleStatus;
import com.goormgb.be.ordercore.match.repository.MatchRepository;
import com.goormgb.be.ordercore.match.utils.SalesOpenUtils;

@ExtendWith(MockitoExtension.class)
class MatchStatusSchedulerTest {

	@Mock
	private MatchRepository matchRepository;

	private SalesOpenUtils salesOpenUtils;
	private MatchStatusScheduler scheduler;

	@BeforeEach
	void setUp() {
		salesOpenUtils = new SalesOpenUtils();
		scheduler = new MatchStatusScheduler(matchRepository, salesOpenUtils);
	}

	// Match 생성 헬퍼

	private Match createMatch(Instant matchAt, SaleStatus status) {
		Match match = Match.builder()
				.matchAt(matchAt)
				.homeClub(null)
				.awayClub(null)
				.stadium(null)
				.saleStatus(status)
				.build();
		return match;
	}

	@Nested
	@DisplayName("openSales — UPCOMING → ON_SALE 전환 (매일 오전 11시 실행)")
	class OpenSales {

		@Test
		@DisplayName("salesOpenAt이 현재보다 이전인 UPCOMING 경기는 ON_SALE로 전환된다")
		void opensMatchWhenSalesOpenAtPassed() {
			// matchAt = 오늘 + 1일 → salesOpenAt = 오늘 - 6일 11:00 (이미 지남)
			Instant matchAt = Instant.now().plus(1, ChronoUnit.DAYS);
			Match match = createMatch(matchAt, SaleStatus.UPCOMING);

			when(matchRepository.findBySaleStatus(SaleStatus.UPCOMING)).thenReturn(List.of(match));

			scheduler.openSales();

			assertThat(match.getSaleStatus()).isEqualTo(SaleStatus.ON_SALE);
		}

		@Test
		@DisplayName("salesOpenAt이 아직 안 된 UPCOMING 경기는 상태가 유지된다")
		void doesNotOpenMatchWhenSalesOpenAtNotYet() {
			// matchAt = 오늘 + 10일 → salesOpenAt = 오늘 + 3일 11:00 (아직 안 지남)
			Instant matchAt = Instant.now().plus(10, ChronoUnit.DAYS);
			Match match = createMatch(matchAt, SaleStatus.UPCOMING);

			when(matchRepository.findBySaleStatus(SaleStatus.UPCOMING)).thenReturn(List.of(match));

			scheduler.openSales();

			assertThat(match.getSaleStatus()).isEqualTo(SaleStatus.UPCOMING);
		}

		@Test
		@DisplayName("UPCOMING 경기가 없으면 아무 처리도 하지 않는다")
		void doesNothingWhenNoUpcomingMatches() {
			when(matchRepository.findBySaleStatus(SaleStatus.UPCOMING)).thenReturn(List.of());

			scheduler.openSales();

			verifyNoMoreInteractions(matchRepository);
		}

		@Test
		@DisplayName("여러 UPCOMING 경기 중 오픈 시각이 지난 것만 ON_SALE로 전환된다")
		void onlyOpensMatchesPastSalesOpenAt() {
			Instant pastMatchAt = Instant.now().plus(1, ChronoUnit.DAYS);
			Instant futureMatchAt = Instant.now().plus(10, ChronoUnit.DAYS);

			Match shouldOpen = createMatch(pastMatchAt, SaleStatus.UPCOMING);
			Match shouldStay = createMatch(futureMatchAt, SaleStatus.UPCOMING);

			when(matchRepository.findBySaleStatus(SaleStatus.UPCOMING))
					.thenReturn(List.of(shouldOpen, shouldStay));

			scheduler.openSales();

			assertThat(shouldOpen.getSaleStatus()).isEqualTo(SaleStatus.ON_SALE);
			assertThat(shouldStay.getSaleStatus()).isEqualTo(SaleStatus.UPCOMING);
		}
	}

	@Nested
	@DisplayName("closeEndedMatches — → ENDED 전환 (매일 자정 00:00 실행)")
	class CloseEndedMatches {

		@Test
		@DisplayName("오늘 자정 기준으로 이전 날짜 경기를 ENDED로 벌크 업데이트한다")
		void callsBulkUpdateWithStartOfToday() {
			when(matchRepository.bulkUpdateEndedMatches(any(), eq(SaleStatus.ENDED), eq(SaleStatus.ENDED)))
					.thenReturn(2);

			scheduler.closeEndedMatches();

			ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
			verify(matchRepository).bulkUpdateEndedMatches(
					captor.capture(),
					eq(SaleStatus.ENDED),
					eq(SaleStatus.ENDED)
			);

			// 전달된 시각이 오늘 자정(UTC 00:00:00)인지 확인
			Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
			assertThat(captor.getValue()).isEqualTo(startOfToday);
		}

		@Test
		@DisplayName("ENDED로 전환할 경기가 없어도 예외가 발생하지 않는다")
		void doesNotThrowWhenNoMatchesToEnd() {
			when(matchRepository.bulkUpdateEndedMatches(any(), any(), any())).thenReturn(0);

			assertThatCode(() -> scheduler.closeEndedMatches()).doesNotThrowAnyException();
		}
	}
}
