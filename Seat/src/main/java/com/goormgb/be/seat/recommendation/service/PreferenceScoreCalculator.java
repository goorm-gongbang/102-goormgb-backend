package com.goormgb.be.seat.recommendation.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.block.entity.Block;

/**
 * 사용자의 온보딩 선호 정보를 기반으로 블럭별 "선호도 점수"를 계산하는 컴포넌트.
 *
 * <p>연석 개수 차이가 크지 않을 때(±10 이내) 어떤 블럭을 더 위로 올릴지 결정하는 보조 점수이다.</p>
 *
 * <h3>점수 구성 (최대 70점)</h3>
 * <ul>
 *   <li><b>뷰포인트 선호</b> — 유저가 온보딩에서 선택한 관람 시야 우선순위
 *       <br>1순위: 30점, 2순위: 20점, 3순위: 10점</li>
 *   <li><b>응원 구단 일치</b> — 유저 응원구단이 경기 참가팀이고, 블럭이 해당 팀 area(홈/어웨이)에 속하면 25점</li>
 *   <li><b>응원석 근접 선호</b> — NEAR이면 응원석 가까운 블럭(cheerRank ≤ 3)에 15점,
 *       FAR이면 먼 블럭(cheerRank > 3)에 15점, ANY면 0점</li>
 * </ul>
 */
@Component
public class PreferenceScoreCalculator {

	private static final int VIEWPOINT_1ST_WEIGHT = 30;
	private static final int VIEWPOINT_2ND_WEIGHT = 20;
	private static final int VIEWPOINT_3RD_WEIGHT = 10;
	private static final int CLUB_PREFERENCE_WEIGHT = 25;
	private static final int CHEER_PROXIMITY_WEIGHT = 15;

	/**
	 * 블럭 하나에 대한 사용자 선호도 점수를 계산한다.
	 *
	 * @param block      점수를 매길 블럭
	 * @param pref       사용자 온보딩 선호 정보 (응원구단, 응원석 근접 선호)
	 * @param viewpoints 사용자 뷰포인트 우선순위 목록 (1~3순위)
	 * @param match      현재 경기 정보 (홈/어웨이 구단 판별용)
	 * @return 선호도 점수 (0 ~ 70)
	 */
	public int calculatePreferenceScore(
		Block block,
		OnboardingPreference pref,
		List<OnboardingViewpointPriority> viewpoints,
		Match match
	) {
		int score = 0;

		score += calculateViewpointScore(block, viewpoints);
		score += calculateClubPreferenceScore(block, pref, match);
		score += calculateCheerProximityScore(block, pref, match);

		return score;
	}

	private int calculateViewpointScore(Block block, List<OnboardingViewpointPriority> viewpoints) {
		Map<Viewpoint, Integer> viewpointPriorityMap = viewpoints.stream()
			.collect(Collectors.toMap(
				OnboardingViewpointPriority::getViewpoint,
				OnboardingViewpointPriority::getPriority
			));

		Integer priority = viewpointPriorityMap.get(block.getViewpoint());
		if (priority == null) {
			return 0;
		}

		return switch (priority) {
			case 1 -> VIEWPOINT_1ST_WEIGHT;
			case 2 -> VIEWPOINT_2ND_WEIGHT;
			case 3 -> VIEWPOINT_3RD_WEIGHT;
			default -> 0;
		};
	}

	private int calculateClubPreferenceScore(Block block, OnboardingPreference pref, Match match) {
		Long favoriteClubId = pref.getFavoriteClub().getId();
		AreaCode areaCode = block.getArea().getCode();

		if (favoriteClubId.equals(match.getHomeClub().getId()) && areaCode == AreaCode.HOME) {
			return CLUB_PREFERENCE_WEIGHT;
		}

		if (favoriteClubId.equals(match.getAwayClub().getId()) && areaCode == AreaCode.AWAY) {
			return CLUB_PREFERENCE_WEIGHT;
		}

		return 0;
	}

	private int calculateCheerProximityScore(Block block, OnboardingPreference pref, Match match) {
		CheerProximityPref cheerPref = pref.getCheerProximityPref();
		if (cheerPref == CheerProximityPref.ANY) {
			return 0;
		}

		Integer cheerRank = resolveCheerRank(block, pref, match);
		if (cheerRank == null) {
			return 0;
		}

		if (cheerPref == CheerProximityPref.NEAR) {
			return cheerRank <= 3 ? CHEER_PROXIMITY_WEIGHT : 0;
		}

		if (cheerPref == CheerProximityPref.FAR) {
			return cheerRank > 3 ? CHEER_PROXIMITY_WEIGHT : 0;
		}

		return 0;
	}

	private Integer resolveCheerRank(Block block, OnboardingPreference pref, Match match) {
		Long favoriteClubId = pref.getFavoriteClub().getId();

		if (favoriteClubId.equals(match.getHomeClub().getId())) {
			return block.getHomeCheerRank();
		}

		if (favoriteClubId.equals(match.getAwayClub().getId())) {
			return block.getAwayCheerRank();
		}

		return block.getHomeCheerRank();
	}
}
