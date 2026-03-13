package com.goormgb.be.ordercore.onboarding.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.club.repository.ClubRepository;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreference;
import com.goormgb.be.domain.onboarding.entity.OnboardingPreferredBlock;
import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.PriceMode;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferenceRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingPreferredBlockRepository;
import com.goormgb.be.domain.onboarding.repository.OnboardingViewpointPriorityRepository;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.onboarding.dto.OnboardingPreferenceItemDto;
import com.goormgb.be.ordercore.onboarding.dto.request.OnboardingPreferenceCreateRequest;
import com.goormgb.be.ordercore.onboarding.dto.request.OnboardingPreferenceUpdateRequest;
import com.goormgb.be.ordercore.onboarding.dto.response.OnboardingPreferenceCreateResponse;
import com.goormgb.be.ordercore.onboarding.dto.response.OnboardingPreferenceGetResponse;
import com.goormgb.be.ordercore.onboarding.dto.response.OnboardingStatusGetResponse;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingPreferenceService {

	private final OnboardingPreferenceRepository onboardingPreferenceRepository;
	private final OnboardingViewpointPriorityRepository viewpointPriorityRepository;
	private final OnboardingPreferredBlockRepository preferredBlockRepository;
	private final ClubRepository clubRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public OnboardingStatusGetResponse getOnboardingStatus(Long userId) {
		var user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);
		return OnboardingStatusGetResponse.from(user);
	}

	@Transactional(readOnly = true)
	public OnboardingPreferenceGetResponse getPreferences(Long userId) {
		userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);

		var preference = onboardingPreferenceRepository.findByUserIdOrThrow(userId, ErrorCode.PREFERENCE_NOT_FOUND);
		var viewpointPriorities = viewpointPriorityRepository.findAllByUserIdOrderByPriorityAsc(userId);
		var preferredBlocks = preferredBlockRepository.findAllByUserId(userId);

		return OnboardingPreferenceGetResponse.from(preference, viewpointPriorities, preferredBlocks);
	}

	@Transactional
	public OnboardingPreferenceCreateResponse createPreferences(
		Long userId,
		OnboardingPreferenceCreateRequest request
	) {
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);

		Preconditions.validate(!user.isCompletedOnboarding(), ErrorCode.ONBOARDING_ALREADY_COMPLETED);

		// 검증
		validateFavoriteClub(request.favoriteClubId());
		validateCheerProximityPref(request.cheerProximityPref());
		validatePreferences(request.preferences());
		validatePreferredBlocks(request.preferredBlockIds());

		// 1순위 항목에서 옵셔널 필드 추출
		OnboardingPreferenceItemDto firstPref = request.preferences().get(0);
		validatePrice(firstPref.priceMode(), firstPref.priceMin(), firstPref.priceMax());

		// 저장
		Club club = clubRepository.findById(request.favoriteClubId())
			.orElseThrow(() -> new CustomException(ErrorCode.CLUB_NOT_FOUND));

		savePreference(user, club, request.cheerProximityPref(), firstPref);
		saveViewpointPriorities(user, request.preferences());
		savePreferredBlocks(user, request.preferredBlockIds());

		// 마케팅 동의
		applyMarketingConsent(user, request.marketingConsent());

		// 온보딩 완료
		user.completeOnboarding();

		return OnboardingPreferenceCreateResponse.from(user);
	}

	@Transactional
	public void updatePreferences(Long userId, OnboardingPreferenceUpdateRequest request) {
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);

		// 검증
		validateFavoriteClub(request.favoriteClubId());
		validateCheerProximityPref(request.cheerProximityPref());
		validatePreferences(request.preferences());
		validatePreferredBlocks(request.preferredBlockIds());

		OnboardingPreferenceItemDto firstPref = request.preferences().get(0);
		validatePrice(firstPref.priceMode(), firstPref.priceMin(), firstPref.priceMax());

		Club club = clubRepository.findById(request.favoriteClubId())
			.orElseThrow(() -> new CustomException(ErrorCode.CLUB_NOT_FOUND));

		// preference: update 또는 신규 생성
		var existingPreference = onboardingPreferenceRepository.findByUserId(userId);
		if (existingPreference.isPresent()) {
			existingPreference.get().update(
				club,
				request.cheerProximityPref(),
				firstPref.seatHeight(),
				firstPref.section(),
				firstPref.seatPositionPref(),
				firstPref.environmentPref(),
				firstPref.moodPref(),
				firstPref.obstructionSensitivity(),
				firstPref.priceMode(),
				firstPref.priceMin(),
				firstPref.priceMax()
			);
		} else {
			savePreference(user, club, request.cheerProximityPref(), firstPref);
		}

		// viewpoint priorities: delete + insert
		viewpointPriorityRepository.deleteAllByUserId(userId);
		saveViewpointPriorities(user, request.preferences());

		// preferred blocks: delete + insert
		preferredBlockRepository.deleteAllByUserId(userId);
		savePreferredBlocks(user, request.preferredBlockIds());

		user.completeOnboarding();
	}

	// ── 저장 헬퍼 ──

	private void savePreference(User user, Club club, CheerProximityPref cheerProximityPref,
		OnboardingPreferenceItemDto pref) {
		onboardingPreferenceRepository.save(
			OnboardingPreference.builder()
				.user(user)
				.favoriteClub(club)
				.cheerProximityPref(cheerProximityPref)
				.seatHeight(pref.seatHeight())
				.section(pref.section())
				.seatPositionPref(pref.seatPositionPref())
				.environmentPref(pref.environmentPref())
				.moodPref(pref.moodPref())
				.obstructionSensitivity(pref.obstructionSensitivity())
				.priceMode(pref.priceMode())
				.priceMin(pref.priceMin())
				.priceMax(pref.priceMax())
				.build()
		);
	}

	private void saveViewpointPriorities(User user, List<OnboardingPreferenceItemDto> preferences) {
		var entities = preferences.stream()
			.map(pref -> OnboardingViewpointPriority.builder()
				.user(user)
				.priority(pref.priority())
				.viewpoint(pref.viewpoint())
				.build())
			.toList();

		viewpointPriorityRepository.saveAll(entities);
	}

	private void savePreferredBlocks(User user, List<Long> blockIds) {
		var entities = blockIds.stream()
			.map(blockId -> OnboardingPreferredBlock.builder()
				.user(user)
				.blockId(blockId)
				.build())
			.toList();

		preferredBlockRepository.saveAll(entities);
	}

	// ── 검증 ──

	private void validateFavoriteClub(Long favoriteClubId) {
		Preconditions.validate(favoriteClubId != null, ErrorCode.MISSING_REQUIRED_PREFERENCE_FIELD);
		Preconditions.validate(clubRepository.existsById(favoriteClubId), ErrorCode.CLUB_NOT_FOUND);
	}

	private void validateCheerProximityPref(CheerProximityPref cheerProximityPref) {
		Preconditions.validate(cheerProximityPref != null, ErrorCode.MISSING_REQUIRED_PREFERENCE_FIELD);
	}

	private void validatePreferences(List<OnboardingPreferenceItemDto> preferences) {
		Preconditions.validate(
			preferences != null && !preferences.isEmpty() && preferences.size() <= 3,
			ErrorCode.INVALID_VIEWPOINT_PRIORITY_COUNT
		);

		// priority 연속성 검증 (1부터 시작, 연속)
		Set<Integer> priorities = preferences.stream()
			.map(OnboardingPreferenceItemDto::priority)
			.collect(Collectors.toSet());

		for (int i = 1; i <= preferences.size(); i++) {
			Preconditions.validate(priorities.contains(i), ErrorCode.INVALID_VIEWPOINT_PRIORITY_SEQUENCE);
		}

		// viewpoint 중복 검증
		Set<Viewpoint> viewpoints = new HashSet<>();
		for (OnboardingPreferenceItemDto pref : preferences) {
			Preconditions.validate(pref.viewpoint() != null, ErrorCode.MISSING_REQUIRED_PREFERENCE_FIELD);
			Preconditions.validate(viewpoints.add(pref.viewpoint()), ErrorCode.DUPLICATE_PREFERENCE_VIEWPOINT);
		}
	}

	private void validatePreferredBlocks(List<Long> blockIds) {
		Preconditions.validate(
			blockIds != null && !blockIds.isEmpty() && blockIds.size() <= 10,
			ErrorCode.INVALID_PREFERRED_BLOCK_COUNT
		);

		Set<Long> uniqueBlockIds = new HashSet<>(blockIds);
		Preconditions.validate(uniqueBlockIds.size() == blockIds.size(), ErrorCode.DUPLICATE_PREFERRED_BLOCK);
	}

	private void validatePrice(PriceMode priceMode, Integer priceMin, Integer priceMax) {
		if (priceMode == PriceMode.RANGE) {
			if (priceMin != null && priceMax != null) {
				Preconditions.validate(priceMin <= priceMax, ErrorCode.INVALID_PRICE_RANGE);
			}
		}
	}

	private void applyMarketingConsent(User user, OnboardingPreferenceCreateRequest.MarketingConsent marketingConsent) {
		Preconditions.validate(marketingConsent != null && marketingConsent.marketingAgreed() != null,
			ErrorCode.INVALID_MARKETING_CONSENT);

		user.updateMarketingConsent(marketingConsent.marketingAgreed());
	}
}
