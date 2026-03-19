package com.goormgb.be.seat.recommendation.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.fixture.BlockFixture;
import com.goormgb.be.seat.fixture.CommonFixture;
import com.goormgb.be.seat.fixture.OnboardingFixture;
import com.goormgb.be.user.entity.User;

class PreferenceScoreCalculatorTest {

	private final PreferenceScoreCalculator calculator = new PreferenceScoreCalculator();

	@Test
	@DisplayName("뷰포인트 1순위 블럭은 30점을 받는다")
	void 뷰포인트_1순위_30점() {
		// given
		Club lgClub = CommonFixture.lgClub();
		Club doosanClub = CommonFixture.doosanClub();
		User user = CommonFixture.user(1L);
		Match match = CommonFixture.match(lgClub, doosanClub);
		Block block = BlockFixture.block(205L, "205", AreaCode.OUTFIELD, Viewpoint.OUTFIELD_C, 70, 70);

		// when
		int score = calculator.calculatePreferenceScore(block,
			OnboardingFixture.preference(user, CommonFixture.samsungClub(), CheerProximityPref.ANY),
			List.of(OnboardingFixture.viewpointPriority(user, Viewpoint.OUTFIELD_C, 1)),
			match);

		// then
		assertThat(score).isEqualTo(30);
	}

	@Test
	@DisplayName("응원구단이 홈팀이고 블럭이 HOME이면 구단 가중치 25점을 받는다")
	void 응원구단_홈팀_HOME블럭_25점() {
		// given
		Club lgClub = CommonFixture.lgClub();
		Club doosanClub = CommonFixture.doosanClub();
		User user = CommonFixture.user(1L);
		Match match = CommonFixture.match(lgClub, doosanClub);
		Block block = BlockFixture.block(205L, "205", AreaCode.HOME, Viewpoint.INFIELD_1B);

		// when
		int score = calculator.calculatePreferenceScore(block,
			OnboardingFixture.preference(user, lgClub, CheerProximityPref.ANY),
			List.of(), match);

		// then
		assertThat(score).isEqualTo(25);
	}

	@Test
	@DisplayName("NEAR 선호 + 응원석 가까운 블럭(rank<=3)이면 15점을 받는다")
	void NEAR_응원석_가까운_블럭_15점() {
		// given
		Club lgClub = CommonFixture.lgClub();
		Club doosanClub = CommonFixture.doosanClub();
		User user = CommonFixture.user(1L);
		Match match = CommonFixture.match(lgClub, doosanClub);
		Block block = BlockFixture.block(205L, "205", AreaCode.HOME, Viewpoint.INFIELD_1B, 2, 80);

		// when
		int score = calculator.calculatePreferenceScore(block,
			OnboardingFixture.preference(user, lgClub, CheerProximityPref.NEAR),
			List.of(), match);

		// then
		// clubPref(25) + cheerProximity(15) = 40
		assertThat(score).isEqualTo(40);
	}

	@Test
	@DisplayName("FAR 선호 + 응원석 먼 블럭(rank>3)이면 15점을 받는다")
	void FAR_응원석_먼_블럭_15점() {
		// given
		Club lgClub = CommonFixture.lgClub();
		Club doosanClub = CommonFixture.doosanClub();
		User user = CommonFixture.user(1L);
		Match match = CommonFixture.match(lgClub, doosanClub);
		Block block = BlockFixture.block(205L, "205", AreaCode.OUTFIELD, Viewpoint.OUTFIELD_C, 70, 70);

		// when
		int score = calculator.calculatePreferenceScore(block,
			OnboardingFixture.preference(user, lgClub, CheerProximityPref.FAR),
			List.of(), match);

		// then
		assertThat(score).isEqualTo(15);
	}

	@Test
	@DisplayName("모든 조건이 맞으면 최대 점수를 받는다")
	void 모든_조건_최대점수() {
		// given
		Club lgClub = CommonFixture.lgClub();
		Club doosanClub = CommonFixture.doosanClub();
		User user = CommonFixture.user(1L);
		Match match = CommonFixture.match(lgClub, doosanClub);
		Block block = BlockFixture.block(205L, "205", AreaCode.HOME, Viewpoint.INFIELD_1B);

		// when
		int score = calculator.calculatePreferenceScore(block,
			OnboardingFixture.preference(user, lgClub, CheerProximityPref.NEAR),
			List.of(OnboardingFixture.viewpointPriority(user, Viewpoint.INFIELD_1B, 1)),
			match);

		// then
		// viewpoint(30) + clubPref(25) + cheerProximity(15) = 70
		assertThat(score).isEqualTo(70);
	}

	@Test
	@DisplayName("비참가 구단 팬은 구단 가중치를 받지 않는다")
	void 비참가_구단_팬_구단가중치_없음() {
		// given
		Club lgClub = CommonFixture.lgClub();
		Club doosanClub = CommonFixture.doosanClub();
		Club samsungClub = CommonFixture.samsungClub();
		User user = CommonFixture.user(1L);
		Match match = CommonFixture.match(lgClub, doosanClub);
		Block block = BlockFixture.block(205L, "205", AreaCode.HOME, Viewpoint.INFIELD_1B);

		// when
		int score = calculator.calculatePreferenceScore(block,
			OnboardingFixture.preference(user, samsungClub, CheerProximityPref.ANY),
			List.of(), match);

		// then
		assertThat(score).isEqualTo(0);
	}
}
