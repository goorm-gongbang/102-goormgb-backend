package com.goormgb.be.seat.recommendation.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.domain.stadium.entity.Stadium;
import com.goormgb.be.seat.area.entity.Area;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.enums.SectionCode;
import com.goormgb.be.user.entity.User;

import org.springframework.test.util.ReflectionTestUtils;

class PreferenceScoreCalculatorTest {

	private final PreferenceScoreCalculator calculator = new PreferenceScoreCalculator();

	private Club createClub(Long id, String name) {
		Club club = Club.builder()
			.koName(name)
			.enName(name)
			.logoImg("logo.png")
			.clubColor("#000")
			.build();
		ReflectionTestUtils.setField(club, "id", id);
		return club;
	}

	private User createUser(Long id) {
		User user = User.builder().build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Match createMatch(Club home, Club away) {
		Stadium stadium = Stadium.builder()
			.region("서울")
			.koName("잠실")
			.enName("Jamsil")
			.address("서울시")
			.build();
		return Match.create(Instant.now(), home, away, stadium, SaleStatus.ON_SALE);
	}

	private Block createBlock(AreaCode areaCode, Viewpoint viewpoint, Integer homeCheerRank, Integer awayCheerRank) {
		Area area = Area.builder().code(areaCode).name(areaCode.getDescription()).build();
		Section section = Section.builder().area(area).code(SectionCode.ORANGE).name("오렌지석").build();
		return Block.builder()
			.area(area)
			.section(section)
			.blockCode("205")
			.viewpoint(viewpoint)
			.homeCheerRank(homeCheerRank)
			.awayCheerRank(awayCheerRank)
			.build();
	}

	private OnboardingPreference createPref(User user, Club favoriteClub, CheerProximityPref cheerPref) {
		return OnboardingPreference.builder()
			.user(user)
			.favoriteClub(favoriteClub)
			.cheerProximityPref(cheerPref)
			.build();
	}

	private OnboardingViewpointPriority createViewpointPriority(User user, Viewpoint viewpoint, int priority) {
		return OnboardingViewpointPriority.builder()
			.user(user)
			.viewpoint(viewpoint)
			.priority(priority)
			.build();
	}

	@Test
	@DisplayName("뷰포인트 1순위 블럭은 30점을 받는다")
	void 뷰포인트_1순위_30점() {
		// given
		Club lgClub = createClub(1L, "LG 트윈스");
		Club doosanClub = createClub(2L, "두산 베어스");
		User user = createUser(1L);
		Match match = createMatch(lgClub, doosanClub);
		Block block = createBlock(AreaCode.OUTFIELD, Viewpoint.OUTFIELD_C, 70, 70);
		OnboardingPreference pref = createPref(user, createClub(3L, "삼성 라이온즈"), CheerProximityPref.ANY);
		List<OnboardingViewpointPriority> viewpoints = List.of(
			createViewpointPriority(user, Viewpoint.OUTFIELD_C, 1)
		);

		// when
		int score = calculator.calculatePreferenceScore(block, pref, viewpoints, match);

		// then
		assertThat(score).isEqualTo(30);
	}

	@Test
	@DisplayName("응원구단이 홈팀이고 블럭이 HOME이면 구단 가중치 25점을 받는다")
	void 응원구단_홈팀_HOME블럭_25점() {
		// given
		Club lgClub = createClub(1L, "LG 트윈스");
		Club doosanClub = createClub(2L, "두산 베어스");
		User user = createUser(1L);
		Match match = createMatch(lgClub, doosanClub);
		Block block = createBlock(AreaCode.HOME, Viewpoint.INFIELD_1B, 1, 81);
		OnboardingPreference pref = createPref(user, lgClub, CheerProximityPref.ANY);

		// when
		int score = calculator.calculatePreferenceScore(block, pref, List.of(), match);

		// then
		assertThat(score).isEqualTo(25);
	}

	@Test
	@DisplayName("NEAR 선호 + 응원석 가까운 블럭(rank<=3)이면 15점을 받는다")
	void NEAR_응원석_가까운_블럭_15점() {
		// given
		Club lgClub = createClub(1L, "LG 트윈스");
		Club doosanClub = createClub(2L, "두산 베어스");
		User user = createUser(1L);
		Match match = createMatch(lgClub, doosanClub);
		Block block = createBlock(AreaCode.HOME, Viewpoint.INFIELD_1B, 2, 80);
		OnboardingPreference pref = createPref(user, lgClub, CheerProximityPref.NEAR);

		// when
		int score = calculator.calculatePreferenceScore(block, pref, List.of(), match);

		// then
		// clubPref(25) + cheerProximity(15) = 40
		assertThat(score).isEqualTo(40);
	}

	@Test
	@DisplayName("FAR 선호 + 응원석 먼 블럭(rank>3)이면 15점을 받는다")
	void FAR_응원석_먼_블럭_15점() {
		// given
		Club lgClub = createClub(1L, "LG 트윈스");
		Club doosanClub = createClub(2L, "두산 베어스");
		User user = createUser(1L);
		Match match = createMatch(lgClub, doosanClub);
		Block block = createBlock(AreaCode.OUTFIELD, Viewpoint.OUTFIELD_C, 70, 70);
		OnboardingPreference pref = createPref(user, lgClub, CheerProximityPref.FAR);

		// when
		int score = calculator.calculatePreferenceScore(block, pref, List.of(), match);

		// then
		assertThat(score).isEqualTo(15);
	}

	@Test
	@DisplayName("모든 조건이 맞으면 최대 점수를 받는다")
	void 모든_조건_최대점수() {
		// given
		Club lgClub = createClub(1L, "LG 트윈스");
		Club doosanClub = createClub(2L, "두산 베어스");
		User user = createUser(1L);
		Match match = createMatch(lgClub, doosanClub);
		Block block = createBlock(AreaCode.HOME, Viewpoint.INFIELD_1B, 1, 81);
		OnboardingPreference pref = createPref(user, lgClub, CheerProximityPref.NEAR);
		List<OnboardingViewpointPriority> viewpoints = List.of(
			createViewpointPriority(user, Viewpoint.INFIELD_1B, 1)
		);

		// when
		int score = calculator.calculatePreferenceScore(block, pref, viewpoints, match);

		// then
		// viewpoint(30) + clubPref(25) + cheerProximity(15) = 70
		assertThat(score).isEqualTo(70);
	}

	@Test
	@DisplayName("비참가 구단 팬은 구단 가중치를 받지 않는다")
	void 비참가_구단_팬_구단가중치_없음() {
		// given
		Club lgClub = createClub(1L, "LG 트윈스");
		Club doosanClub = createClub(2L, "두산 베어스");
		Club samsungClub = createClub(3L, "삼성 라이온즈");
		User user = createUser(1L);
		Match match = createMatch(lgClub, doosanClub);
		Block block = createBlock(AreaCode.HOME, Viewpoint.INFIELD_1B, 1, 81);
		OnboardingPreference pref = createPref(user, samsungClub, CheerProximityPref.ANY);

		// when
		int score = calculator.calculatePreferenceScore(block, pref, List.of(), match);

		// then
		assertThat(score).isEqualTo(0);
	}
}
