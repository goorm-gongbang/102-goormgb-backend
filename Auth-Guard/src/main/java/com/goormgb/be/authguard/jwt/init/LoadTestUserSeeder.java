package com.goormgb.be.authguard.jwt.init;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.club.repository.ClubRepository;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreferredBlock;
import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferenceRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferredBlockRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingViewpointPriorityRepository;
import com.goormgb.be.user.entity.LoadTestUser;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.repository.LoadTestUserRepository;
import com.goormgb.be.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({"local", "staging", "dev"})
@RequiredArgsConstructor
public class LoadTestUserSeeder implements CommandLineRunner {

	private static final int TOTAL_USERS = 4000;
	private static final String PASSWORD = "1234";
	private static final int BATCH_LOG_INTERVAL = 100;
	private static final int FLUSH_INTERVAL = 100;
	private static final int CLUB_COUNT = 10;

	/**
	 * 시드 데이터 기준 유효 블록 번호 (blockNum)
	 * 좌석 배치도 기반: 내야(1루/3루), 외야, 중앙 프리미엄 전 구역 포함
	 */
	private static final List<Long> VALID_BLOCK_NUMS = List.of(
			1L, // CP (중앙 프리미엄)
			2L, // EX-1 (익사이팅존 1루)
			3L, // EX-3 (익사이팅존 3루)
			101L, 102L, 103L, 104L, 105L, 106L, // 3루 레드
			107L, 108L, 109L,                     // 3루 블루
			110L, 111L, 112L, 113L,               // 1루 퍼플
			114L, 115L, 116L,                     // 1루 블루
			117L, 118L, 119L, 120L, 121L, 122L,   // 1루 레드
			201L, 202L, 203L, 204L,               // 3루 레드 2층
			205L, 206L, 207L, 208L,               // 1루 오렌지
			209L, 210L, 211L,                     // 3루 블루 2층
			212L, 213L, 214L, 215L,               // 3루 퍼플
			216L, 217L, 218L,                     // 1루 블루 2층
			219L, 220L, 221L, 222L,               // 3루 오렌지
			223L, 224L, 225L, 226L,               // 1루 레드 2층
			301L, 302L, 303L, 304L, 305L, 306L, 307L, 308L, 309L, 310L,
			311L, 312L, 313L, 314L, 315L, 316L, 317L, // 1루 네이비
			318L, 319L, 320L, 321L, 322L, 323L, 324L, 325L, 326L, 327L,
			328L, 329L, 330L, 331L, 332L, 333L, 334L, // 3루 네이비
			401L, 402L, 403L, 404L, 405L, 406L, 407L, // 외야 R
			408L, 409L, 410L, 411L, 412L, 413L, 414L, 415L, // 외야 C
			416L, 417L, 418L, 419L, 420L, 421L, 422L  // 외야 L
	);

	/**
	 * 뷰포인트 순환 배열: CENTER → INFIELD_1B → INFIELD_3B → OUTFIELD_L → OUTFIELD_C → OUTFIELD_R
	 * ID마다 시작 인덱스를 1칸씩 밀어 슬라이딩 윈도우로 3개씩 선택
	 */
	private static final Viewpoint[] VIEWPOINTS = Viewpoint.values();

	private final UserRepository userRepository;
	private final LoadTestUserRepository loadTestUserRepository;
	private final ClubRepository clubRepository;
	private final OnboardingPreferenceRepository onboardingPreferenceRepository;
	private final OnboardingPreferredBlockRepository onboardingPreferredBlockRepository;
	private final OnboardingViewpointPriorityRepository onboardingViewpointPriorityRepository;
	private final PasswordEncoder passwordEncoder;
	private final EntityManager entityManager;

	@Override
	@Transactional
	public void run(String... args) {
		List<String> targetLoginIds = LongStream.rangeClosed(1, TOTAL_USERS)
				.mapToObj(String::valueOf)
				.toList();
		Set<String> existingLoginIds = loadTestUserRepository.findExistingLoginIds(targetLoginIds);
		if (existingLoginIds.size() >= TOTAL_USERS) {
			log.info("[LoadTest] 부하테스트 계정 1~{}번 모두 존재 — 시딩 생략", TOTAL_USERS);
			return;
		}

		log.info("[LoadTest] 부하테스트 계정 시딩 시작 (목표: {}개, 기존: {}개)", TOTAL_USERS, existingLoginIds.size());

		List<Club> clubs = loadClubs();
		if (clubs.isEmpty()) {
			log.warn("[LoadTest] 클럽 데이터가 없어 시딩을 중단합니다. 시드 데이터를 먼저 확인해 주세요.");
			return;
		}

		String encodedPassword = passwordEncoder.encode(PASSWORD);
		int created = 0;

		for (int i = 1; i <= TOTAL_USERS; i++) {
			String loginId = String.valueOf(i);

			if (existingLoginIds.contains(loginId)) {
				continue;
			}

			try {
				User user = User.builder()
						.email(loginId + "@loadtest.com")
						.nickname("loadtest_" + loginId)
						.build();
				userRepository.save(user);

				LoadTestUser loadTestUser = LoadTestUser.builder()
						.loginId(loginId)
						.passwordHash(encodedPassword)
						.user(user)
						.build();
				loadTestUserRepository.save(loadTestUser);

				seedOnboarding(user, clubs, i);
				user.completeOnboarding();
				user.updateMarketingConsent(true);

				created++;
			} catch (DataIntegrityViolationException e) {
				log.warn("[LoadTest] 동시 부팅으로 인한 중복 충돌 — loginId: {} (스킵)", loginId);
			}

			if (created % FLUSH_INTERVAL == 0) {
				entityManager.flush();
				entityManager.clear();
			}

			if (created % BATCH_LOG_INTERVAL == 0) {
				log.info("[LoadTest] 부하테스트 계정 시딩 진행중... {}개 생성 완료", created);
			}
		}

		log.info("[LoadTest] 부하테스트 계정 시딩 완료 — 총 {}개 신규 생성 (온보딩 포함)", created);
	}

	private void seedOnboarding(User user, List<Club> clubs, int userId) {
		// 1. 응원 구단: 1→10 순환
		Club favoriteClub = clubs.get((userId - 1) % clubs.size());

		// 2. 응원석 근접: 홀수=NEAR(인접), 짝수=FAR(비인접)
		CheerProximityPref cheerPref = (userId % 2 == 1)
				? CheerProximityPref.NEAR
				: CheerProximityPref.FAR;

		// 3. 나머지 옵션 전부 null → 엔티티에서 기본값 ANY/NORMAL 적용
		OnboardingPreference preference = OnboardingPreference.builder()
				.user(user)
				.favoriteClub(favoriteClub)
				.cheerProximityPref(cheerPref)
				.build();
		onboardingPreferenceRepository.save(preference);

		// 4. 뷰포인트 우선순위: 슬라이딩 윈도우 3개씩
		//    ID 1: CENTER, INFIELD_1B, INFIELD_3B
		//    ID 2: INFIELD_1B, INFIELD_3B, OUTFIELD_L
		//    ID 3: INFIELD_3B, OUTFIELD_L, OUTFIELD_C  ...
		int startIdx = (userId - 1) % VIEWPOINTS.length;
		for (int p = 0; p < 3; p++) {
			Viewpoint vp = VIEWPOINTS[(startIdx + p) % VIEWPOINTS.length];
			OnboardingViewpointPriority priority = OnboardingViewpointPriority.builder()
					.user(user)
					.priority(p + 1)
					.viewpoint(vp)
					.build();
			onboardingViewpointPriorityRepository.save(priority);
		}

		// 5. 선호 블록: 랜덤 1~10개, 중복 없이 셔플
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int blockCount = random.nextInt(1, 11);
		List<Long> shuffledBlocks = new ArrayList<>(VALID_BLOCK_NUMS);
		Collections.shuffle(shuffledBlocks, random);
		for (int b = 0; b < blockCount; b++) {
			OnboardingPreferredBlock block = OnboardingPreferredBlock.builder()
					.user(user)
					.blockId(shuffledBlocks.get(b))
					.build();
			onboardingPreferredBlockRepository.save(block);
		}
	}

	private List<Club> loadClubs() {
		return LongStream.rangeClosed(1, CLUB_COUNT)
				.mapToObj(clubRepository::getReferenceById)
				.toList();
	}
}
