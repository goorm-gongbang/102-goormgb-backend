package com.goormgb.be.authguard.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.authguard.auth.dto.RefreshTokenInfo;
import com.goormgb.be.authguard.jwt.config.JwtProperties;
import com.goormgb.be.authguard.jwt.provider.JwtTokenProvider;
import com.goormgb.be.authguard.jwt.repository.RefreshTokenRepository;
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
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.user.entity.DevUser;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.enums.UserStatus;
import com.goormgb.be.user.repository.DevUserRepository;
import com.goormgb.be.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DevAuthService {

	private static final String DEFAULT_AUTHORITY = "ROLE_USER";
	private static final Viewpoint[] VIEWPOINTS = Viewpoint.values();
	private static final List<Long> VALID_BLOCK_NUMS = List.of(
			1L, 2L, 3L,
			101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L,
			110L, 111L, 112L, 113L, 114L, 115L, 116L, 117L, 118L, 119L, 120L, 121L, 122L,
			201L, 202L, 203L, 204L, 205L, 206L, 207L, 208L, 209L, 210L, 211L,
			212L, 213L, 214L, 215L, 216L, 217L, 218L, 219L, 220L, 221L, 222L, 223L, 224L, 225L, 226L,
			301L, 302L, 303L, 304L, 305L, 306L, 307L, 308L, 309L, 310L,
			311L, 312L, 313L, 314L, 315L, 316L, 317L, 318L, 319L, 320L, 321L, 322L, 323L, 324L,
			325L, 326L, 327L, 328L, 329L, 330L, 331L, 332L, 333L, 334L,
			401L, 402L, 403L, 404L, 405L, 406L, 407L, 408L, 409L, 410L,
			411L, 412L, 413L, 414L, 415L, 416L, 417L, 418L, 419L, 420L, 421L, 422L
	);
	private static final int PREFERRED_BLOCK_COUNT = 10;

	private final UserRepository userRepository;
	private final DevUserRepository devUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final RefreshTokenRepository refreshTokenRepository;
	private final ClubRepository clubRepository;
	private final OnboardingPreferenceRepository onboardingPreferenceRepository;
	private final OnboardingPreferredBlockRepository onboardingPreferredBlockRepository;
	private final OnboardingViewpointPriorityRepository onboardingViewpointPriorityRepository;
	private final AccountLockService accountLockService;

	/**
	 * 개발용 회원가입 — 온보딩 정보(응원 구단·응원석·뷰포인트·선호 블록)를 자동 생성하여
	 * 가입 즉시 서비스 이용이 가능하도록 처리한다.
	 */
	@Transactional
	public void signup(String loginId, String password, String nickname, String email) {
		if (devUserRepository.existsByLoginId(loginId)) {
			throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
		}

		User user = User.builder()
				.email(email)
				.nickname(nickname != null ? nickname : loginId)
				.build();
		userRepository.save(user);

		DevUser devUser = DevUser.builder()
				.loginId(loginId)
				.passwordHash(passwordEncoder.encode(password))
				.user(user)
				.build();
		devUserRepository.save(devUser);

		seedOnboarding(user);
		user.completeOnboarding();
		user.updateMarketingConsent(true);

		log.info("Dev user created with onboarding - loginId: {}, userId: {}", loginId, user.getId());
	}

	/**
	 * 온보딩 기본 데이터 자동 시딩:
	 * - 응원 구단: 랜덤 선택
	 * - 응원석 근접: 항상 NEAR (인접 선호)
	 * - 뷰포인트 우선순위: 랜덤 3개
	 * - 선호 블록: 유효 블록 중 랜덤 10개
	 */
	private void seedOnboarding(User user) {
		List<Club> clubs = clubRepository.findAll();
		if (clubs.isEmpty()) {
			log.warn("클럽 데이터가 없어 온보딩 시딩을 건너뜁니다.");
			return;
		}

		ThreadLocalRandom random = ThreadLocalRandom.current();

		// 응원 구단 랜덤 + 응원석 NEAR 고정
		Club favoriteClub = clubs.get(random.nextInt(clubs.size()));
		OnboardingPreference preference = OnboardingPreference.builder()
				.user(user)
				.favoriteClub(favoriteClub)
				.cheerProximityPref(CheerProximityPref.NEAR)
				.build();
		onboardingPreferenceRepository.save(preference);

		// 뷰포인트 우선순위: 랜덤 시작 인덱스로 3개 슬라이딩 윈도우
		int startIdx = random.nextInt(VIEWPOINTS.length);
		for (int p = 0; p < 3; p++) {
			Viewpoint vp = VIEWPOINTS[(startIdx + p) % VIEWPOINTS.length];
			OnboardingViewpointPriority priority = OnboardingViewpointPriority.builder()
					.user(user)
					.priority(p + 1)
					.viewpoint(vp)
					.build();
			onboardingViewpointPriorityRepository.save(priority);
		}

		// 선호 블록: 랜덤 10개
		List<Long> shuffledBlocks = new ArrayList<>(VALID_BLOCK_NUMS);
		Collections.shuffle(shuffledBlocks, random);
		for (int b = 0; b < PREFERRED_BLOCK_COUNT; b++) {
			OnboardingPreferredBlock block = OnboardingPreferredBlock.builder()
					.user(user)
					.blockId(shuffledBlocks.get(b))
					.build();
			onboardingPreferredBlockRepository.save(block);
		}
	}

	@Transactional
	public DevLoginResult login(String loginId, String password, HttpServletRequest request) {
		// 1) 계정 잠금 상태 선 검증 (email/loginId 기준 크리덴셜 스터핑 방어)
		accountLockService.ensureNotLocked(loginId);

		DevUser devUser = devUserRepository.findByLoginId(loginId).orElse(null);
		if (devUser == null) {
			// 존재하지 않는 계정도 실패로 카운트 → enumeration 공격 억제
			accountLockService.recordFailure(loginId);
			throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
		}

		if (!passwordEncoder.matches(password, devUser.getPasswordHash())) {
			accountLockService.recordFailure(loginId);
			throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
		}

		// 성공 시 단기 카운터/잠금 초기화
		accountLockService.resetOnSuccess(loginId);

		User user = devUser.getUser();

		Preconditions.validate(
				user.getStatus() != UserStatus.DEACTIVATE,
				ErrorCode.USER_DEACTIVATED
		);

		user.updateLastLoginAt();

		String sid = UUID.randomUUID().toString();
		String accessToken = jwtTokenProvider.createAccessToken(user.getId(), DEFAULT_AUTHORITY, sid);
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), sid);
		String jti = jwtTokenProvider.getJtiFromToken(refreshToken);

		Instant now = Instant.now();
		int expirationHours = jwtProperties.getRefreshToken().getExpirationHours();

		RefreshTokenInfo tokenInfo = RefreshTokenInfo.builder()
				.userId(user.getId())
				.token(refreshToken)
				.jti(jti)
				.sid(sid)
				.issuedAt(now)
				.expiresAt(now.plus(Duration.ofHours(expirationHours)))
				.userAgent(request.getHeader("User-Agent"))
				.ipAddress(getClientIp(request))
				.build();

		refreshTokenRepository.save(tokenInfo);

		log.info("Dev user logged in - loginId: {}, userId: {}", loginId, user.getId());

		boolean agreementRequired = !Boolean.TRUE.equals(user.getMarketingConsent());
		boolean onboardingRequired = !Boolean.TRUE.equals(user.getOnboardingCompleted());

		return new DevLoginResult(accessToken, refreshToken, agreementRequired, onboardingRequired);
	}

	private String getClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	public record DevLoginResult(String accessToken, String refreshToken, boolean agreementRequired,
								 boolean onboardingRequired) {
	}
}
