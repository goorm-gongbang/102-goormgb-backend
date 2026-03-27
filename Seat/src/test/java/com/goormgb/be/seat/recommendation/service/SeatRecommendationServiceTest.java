package com.goormgb.be.seat.recommendation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferredBlockRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferenceRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingViewpointPriorityRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.fixture.BlockFixture;
import com.goormgb.be.seat.fixture.CommonFixture;
import com.goormgb.be.seat.fixture.OnboardingFixture;
import com.goormgb.be.seat.booking.model.BookingOptions;
import com.goormgb.be.seat.booking.repository.BookingOptionsRedisRepository;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.metrics.SeatMetricsService;
import com.goormgb.be.seat.recommendation.dto.response.BlockRecommendationResponse;
import com.goormgb.be.user.entity.User;

@ExtendWith(MockitoExtension.class)
class SeatRecommendationServiceTest {

	@Mock
	private MatchRepository matchRepository;
	@Mock
	private BookingOptionsRedisRepository bookingOptionsRedisRepository;
	@Mock
	private BlockRepository blockRepository;
	@Mock
	private MatchSeatRepository matchSeatRepository;
	@Mock
	private OnboardingPreferredBlockRepository onboardingPreferredBlockRepository;
	@Mock
	private OnboardingPreferenceRepository onboardingPreferenceRepository;
	@Mock
	private OnboardingViewpointPriorityRepository onboardingViewpointPriorityRepository;
	@Mock
	private ConsecutiveSeatCounter consecutiveSeatCounter;
	@Mock
	private PreferenceScoreCalculator preferenceScoreCalculator;

	@Mock
	private SeatMetricsService seatMetricsService;

	@InjectMocks
	private SeatRecommendationService seatRecommendationService;

	@Test
	@DisplayName("추천 블럭 리스트를 연석 개수 기준으로 정렬하여 반환한다")
	void 추천_블럭_리스트_연석개수_정렬() {
		// given
		Long userId = 1L;
		Long matchId = 1L;
		Club lgClub = CommonFixture.lgClub();
		Club doosanClub = CommonFixture.doosanClub();
		User user = CommonFixture.user(userId);

		Block block205 = BlockFixture.block(205L, "205", AreaCode.HOME, Viewpoint.INFIELD_1B);
		Block block206 = BlockFixture.block(206L, "206", AreaCode.HOME, Viewpoint.INFIELD_1B);

		BookingOptions bookingOptions = new BookingOptions(userId, matchId, true, 5, false, Instant.now());
		Match match = CommonFixture.match(lgClub, doosanClub);

		given(bookingOptionsRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId)).willReturn(bookingOptions);
		given(onboardingPreferredBlockRepository.findBlockIdsByUserId(userId))
			.willReturn(List.of(205L, 206L));
		given(matchRepository.findDetailByIdOrThrow(matchId)).willReturn(match);
		given(blockRepository.findAllByBlockNumInWithSectionAndArea(List.of(205L, 206L)))
			.willReturn(List.of(block205, block206));
		given(onboardingPreferenceRepository.findByUserIdOrThrow(eq(userId), any()))
			.willReturn(OnboardingFixture.preference(user, lgClub, CheerProximityPref.ANY));
		given(onboardingViewpointPriorityRepository.findAllByUserIdOrderByPriorityAsc(userId)).willReturn(List.of());

		given(matchSeatRepository.countRemainingSeatsByMatchIdAndBlockIdIn(eq(matchId), any())).willReturn(List.of());

		// block206이 연석 더 많음 (차이 > 10)
		given(consecutiveSeatCounter.countRealConsecutiveSeats(matchId, 205L, 5)).willReturn(5);
		given(consecutiveSeatCounter.countRealConsecutiveSeats(matchId, 206L, 5)).willReturn(20);

		// when
		BlockRecommendationResponse response = seatRecommendationService.getRecommendedBlocks(matchId, userId);

		// then
		assertThat(response.blocks()).hasSize(2);
		assertThat(response.blocks().get(0).blockCode()).isEqualTo("206");
		assertThat(response.blocks().get(0).rank()).isEqualTo(1);
		assertThat(response.blocks().get(1).blockCode()).isEqualTo("205");
		assertThat(response.blocks().get(1).rank()).isEqualTo(2);
		assertThat(response.ticketCount()).isEqualTo(5);
	}

	@Test
	@DisplayName("연석 개수 차이가 10 이내이면 선호도 점수로 정렬한다")
	void 연석_비슷하면_취향점수_정렬() {
		// given
		Long userId = 1L;
		Long matchId = 1L;
		Club lgClub = CommonFixture.lgClub();
		Club doosanClub = CommonFixture.doosanClub();
		User user = CommonFixture.user(userId);

		Block block205 = BlockFixture.block(205L, "205", AreaCode.HOME, Viewpoint.INFIELD_1B);
		Block block408 = BlockFixture.block(408L, "408", AreaCode.OUTFIELD, Viewpoint.OUTFIELD_C);

		BookingOptions bookingOptions = new BookingOptions(userId, matchId, true, 3, false, Instant.now());
		Match match = CommonFixture.match(lgClub, doosanClub);

		given(bookingOptionsRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId)).willReturn(bookingOptions);
		given(onboardingPreferredBlockRepository.findBlockIdsByUserId(userId))
			.willReturn(List.of(205L, 408L));
		given(matchRepository.findDetailByIdOrThrow(matchId)).willReturn(match);
		given(blockRepository.findAllByBlockNumInWithSectionAndArea(List.of(205L, 408L)))
			.willReturn(List.of(block205, block408));
		given(onboardingPreferenceRepository.findByUserIdOrThrow(eq(userId), any()))
			.willReturn(OnboardingFixture.preference(user, lgClub, CheerProximityPref.NEAR));
		given(onboardingViewpointPriorityRepository.findAllByUserIdOrderByPriorityAsc(userId))
			.willReturn(List.of(OnboardingFixture.viewpointPriority(user, Viewpoint.INFIELD_1B, 1)));

		given(matchSeatRepository.countRemainingSeatsByMatchIdAndBlockIdIn(eq(matchId), any())).willReturn(List.of());

		// 연석 차이 10 이내
		given(consecutiveSeatCounter.countRealConsecutiveSeats(matchId, 205L, 3)).willReturn(12);
		given(consecutiveSeatCounter.countRealConsecutiveSeats(matchId, 408L, 3)).willReturn(15);

		// block205의 선호도 점수가 더 높음
		given(preferenceScoreCalculator.calculatePreferenceScore(eq(block205), any(), any(), any())).willReturn(70);
		given(preferenceScoreCalculator.calculatePreferenceScore(eq(block408), any(), any(), any())).willReturn(15);

		// when
		BlockRecommendationResponse response = seatRecommendationService.getRecommendedBlocks(matchId, userId);

		// then
		assertThat(response.blocks()).hasSize(2);
		assertThat(response.blocks().get(0).blockCode()).isEqualTo("205"); // 선호도 점수 높음
		assertThat(response.blocks().get(1).blockCode()).isEqualTo("408");
	}

	@Test
	@DisplayName("추천 가능한 블럭이 없으면 예외를 발생시킨다")
	void 추천_블럭_없음_예외() {
		// given
		Long userId = 1L;
		Long matchId = 1L;
		Club lgClub = CommonFixture.lgClub();
		Club doosanClub = CommonFixture.doosanClub();
		User user = CommonFixture.user(userId);

		Block block205 = BlockFixture.block(205L, "205", AreaCode.HOME, Viewpoint.INFIELD_1B);

		BookingOptions bookingOptions = new BookingOptions(userId, matchId, true, 5, false, Instant.now());
		Match match = CommonFixture.match(lgClub, doosanClub);

		given(bookingOptionsRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId)).willReturn(bookingOptions);
		given(onboardingPreferredBlockRepository.findBlockIdsByUserId(userId))
			.willReturn(List.of(205L));
		given(matchRepository.findDetailByIdOrThrow(matchId)).willReturn(match);
		given(blockRepository.findAllByBlockNumInWithSectionAndArea(List.of(205L))).willReturn(List.of(block205));
		given(onboardingPreferenceRepository.findByUserIdOrThrow(eq(userId), any()))
			.willReturn(OnboardingFixture.preference(user, lgClub, CheerProximityPref.ANY));
		given(onboardingViewpointPriorityRepository.findAllByUserIdOrderByPriorityAsc(userId)).willReturn(List.of());

		given(matchSeatRepository.countRemainingSeatsByMatchIdAndBlockIdIn(eq(matchId), any())).willReturn(List.of());

		// 모든 블럭에 연석 없음
		given(consecutiveSeatCounter.countRealConsecutiveSeats(matchId, 205L, 5)).willReturn(0);

		// when & then
		assertThatThrownBy(() -> seatRecommendationService.getRecommendedBlocks(matchId, userId))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode()).isEqualTo(ErrorCode.NO_AVAILABLE_BLOCK));
	}
}
