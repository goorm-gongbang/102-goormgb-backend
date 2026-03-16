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
import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferenceRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingViewpointPriorityRepository;
import com.goormgb.be.domain.stadium.entity.Stadium;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.seat.area.entity.Area;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.recommendation.dto.response.BlockRecommendationResponse;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;
import com.goormgb.be.seat.redis.SeatSession;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.enums.SectionCode;
import com.goormgb.be.user.entity.User;

@ExtendWith(MockitoExtension.class)
class SeatRecommendationServiceTest {

	@Mock
	private MatchRepository matchRepository;
	@Mock
	private SeatPreferenceRedisRepository seatPreferenceRedisRepository;
	@Mock
	private BlockRepository blockRepository;
	@Mock
	private OnboardingPreferenceRepository onboardingPreferenceRepository;
	@Mock
	private OnboardingViewpointPriorityRepository onboardingViewpointPriorityRepository;
	@Mock
	private ConsecutiveSeatCounter consecutiveSeatCounter;
	@Mock
	private PreferenceScoreCalculator preferenceScoreCalculator;

	@InjectMocks
	private SeatRecommendationService seatRecommendationService;

	private Club createClub(Long id, String name) {
		Club club = Club.builder()
			.koName(name).enName(name).logoImg("logo.png").clubColor("#000").build();
		ReflectionTestUtils.setField(club, "id", id);
		return club;
	}

	private User createUser(Long id) {
		User user = User.builder().build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Block createBlock(Long id, String blockCode, AreaCode areaCode, Viewpoint viewpoint) {
		Area area = Area.builder().code(areaCode).name(areaCode.getDescription()).build();
		Section section = Section.builder().area(area).code(SectionCode.ORANGE).name("오렌지석").build();
		Block block = Block.builder()
			.area(area).section(section).blockCode(blockCode)
			.viewpoint(viewpoint).homeCheerRank(1).awayCheerRank(81).build();
		ReflectionTestUtils.setField(block, "id", id);
		return block;
	}

	private SeatSession createSeatSession(int ticketCount, List<Long> blockIds) {
		SeatSession session = new SeatSession();
		ReflectionTestUtils.setField(session, "userId", 1L);
		ReflectionTestUtils.setField(session, "matchId", 1L);
		ReflectionTestUtils.setField(session, "recommendationEnabled", true);
		ReflectionTestUtils.setField(session, "ticketCount", ticketCount);
		ReflectionTestUtils.setField(session, "preferredBlockIds", blockIds);
		return session;
	}

	private Match createMatch(Club home, Club away) {
		Stadium stadium = Stadium.builder()
			.region("서울").koName("잠실").enName("Jamsil").address("서울시").build();
		return Match.create(Instant.now(), home, away, stadium, SaleStatus.ON_SALE);
	}

	@Test
	@DisplayName("추천 블럭 리스트를 연석 개수 기준으로 정렬하여 반환한다")
	void 추천_블럭_리스트_연석개수_정렬() {
		// given
		Long userId = 1L;
		Long matchId = 1L;
		Club lgClub = createClub(1L, "LG 트윈스");
		Club doosanClub = createClub(2L, "두산 베어스");
		User user = createUser(userId);

		Block block205 = createBlock(205L, "205", AreaCode.HOME, Viewpoint.INFIELD_1B);
		Block block206 = createBlock(206L, "206", AreaCode.HOME, Viewpoint.INFIELD_1B);

		SeatSession session = createSeatSession(5, List.of(205L, 206L));
		Match match = createMatch(lgClub, doosanClub);
		OnboardingPreference pref = OnboardingPreference.builder()
			.user(user).favoriteClub(lgClub).cheerProximityPref(CheerProximityPref.ANY).build();

		given(seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId)).willReturn(session);
		given(matchRepository.findDetailByIdOrThrow(matchId)).willReturn(match);
		given(blockRepository.findAllByIdInWithSectionAndArea(List.of(205L, 206L)))
			.willReturn(List.of(block205, block206));
		given(onboardingPreferenceRepository.findByUserIdOrThrow(eq(userId), any())).willReturn(pref);
		given(onboardingViewpointPriorityRepository.findAllByUserIdOrderByPriorityAsc(userId)).willReturn(List.of());

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
		Club lgClub = createClub(1L, "LG 트윈스");
		Club doosanClub = createClub(2L, "두산 베어스");
		User user = createUser(userId);

		Block block205 = createBlock(205L, "205", AreaCode.HOME, Viewpoint.INFIELD_1B);
		Block block408 = createBlock(408L, "408", AreaCode.OUTFIELD, Viewpoint.OUTFIELD_C);

		SeatSession session = createSeatSession(3, List.of(205L, 408L));
		Match match = createMatch(lgClub, doosanClub);
		OnboardingPreference pref = OnboardingPreference.builder()
			.user(user).favoriteClub(lgClub).cheerProximityPref(CheerProximityPref.NEAR).build();
		List<OnboardingViewpointPriority> viewpoints = List.of(
			OnboardingViewpointPriority.builder().user(user).viewpoint(Viewpoint.INFIELD_1B).priority(1).build()
		);

		given(seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId)).willReturn(session);
		given(matchRepository.findDetailByIdOrThrow(matchId)).willReturn(match);
		given(blockRepository.findAllByIdInWithSectionAndArea(List.of(205L, 408L)))
			.willReturn(List.of(block205, block408));
		given(onboardingPreferenceRepository.findByUserIdOrThrow(eq(userId), any())).willReturn(pref);
		given(onboardingViewpointPriorityRepository.findAllByUserIdOrderByPriorityAsc(userId)).willReturn(viewpoints);

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
		Club lgClub = createClub(1L, "LG 트윈스");
		Club doosanClub = createClub(2L, "두산 베어스");
		User user = createUser(userId);

		Block block205 = createBlock(205L, "205", AreaCode.HOME, Viewpoint.INFIELD_1B);

		SeatSession session = createSeatSession(5, List.of(205L));
		Match match = createMatch(lgClub, doosanClub);
		OnboardingPreference pref = OnboardingPreference.builder()
			.user(user).favoriteClub(lgClub).cheerProximityPref(CheerProximityPref.ANY).build();

		given(seatPreferenceRedisRepository.getByUserIdAndMatchIdOrThrow(userId, matchId)).willReturn(session);
		given(matchRepository.findDetailByIdOrThrow(matchId)).willReturn(match);
		given(blockRepository.findAllByIdInWithSectionAndArea(List.of(205L))).willReturn(List.of(block205));
		given(onboardingPreferenceRepository.findByUserIdOrThrow(eq(userId), any())).willReturn(pref);
		given(onboardingViewpointPriorityRepository.findAllByUserIdOrderByPriorityAsc(userId)).willReturn(List.of());

		// 모든 블럭에 연석 없음
		given(consecutiveSeatCounter.countRealConsecutiveSeats(matchId, 205L, 5)).willReturn(0);

		// when & then
		assertThatThrownBy(() -> seatRecommendationService.getRecommendedBlocks(matchId, userId))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> assertThat(((CustomException)ex).getErrorCode()).isEqualTo(ErrorCode.NO_AVAILABLE_BLOCK));
	}
}
