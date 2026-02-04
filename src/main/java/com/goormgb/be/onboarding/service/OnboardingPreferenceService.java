package com.goormgb.be.onboarding.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.onboarding.dto.OnboardingPreferenceDto;
import com.goormgb.be.onboarding.dto.request.OnboardingPreferenceCreateRequest;
import com.goormgb.be.onboarding.dto.request.OnboardingPreferenceUpdateRequest;
import com.goormgb.be.onboarding.dto.response.OnboardingPreferenceCreateResponse;
import com.goormgb.be.onboarding.dto.response.OnboardingPreferenceGetResponse;
import com.goormgb.be.onboarding.entity.OnboardingPreference;
import com.goormgb.be.onboarding.enums.SeatHeight;
import com.goormgb.be.onboarding.enums.Section;
import com.goormgb.be.onboarding.enums.Viewpoint;
import com.goormgb.be.onboarding.repository.OnboardingPreferenceRepository;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingPreferenceService {
	private final OnboardingPreferenceRepository onboardingPreferenceRepository;
	private final UserRepository userRepository;

	public OnboardingPreferenceGetResponse getPreferences(Long userId) {
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);

		var preferences = onboardingPreferenceRepository.findAllByUserIdOrderByPriorityAsc(userId);

		return OnboardingPreferenceGetResponse.from(preferences);
	}

	public OnboardingPreferenceCreateResponse createPreferences(Long userId, OnboardingPreferenceCreateRequest request) {
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);

		// 검증
		validatePreferences(request.preferences());

		// 저장
		var entities = request
			.preferences()
			.stream()
			.map(preference -> toEntity(user, preference))
			.toList();

		onboardingPreferenceRepository.saveAll(entities);

		// 마케팅 동의
		applyMarketingConsent(user, request.marketingConsent());

		// 온보딩 완료
		user.completeOnboarding();

		return OnboardingPreferenceCreateResponse.from(user);
	}

	public void updatePreferences(Long userId, OnboardingPreferenceUpdateRequest request) {
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);

		// 검증
		validatePreferences(request.preferences());

		// 저장
		replacePreferences(userId, request.preferences());

		// 온보딩 수정 완료
		user.completeOnboarding();
	}

	private void replacePreferences(Long userId, List<OnboardingPreferenceDto> preferences) {
		var existingPreferences = onboardingPreferenceRepository.findAllByUserIdOrderByPriorityAsc(userId);

		for (OnboardingPreferenceDto pref : preferences) {
			OnboardingPreference preferenceToUpdate = existingPreferences
				.stream()
				.filter(p -> p
					.getPriority()
					.equals(pref.priority()))
				.findFirst()
				.orElseThrow(() -> new CustomException(ErrorCode.PREFERENCE_NOT_FOUND_FOR_UPDATE));

			preferenceToUpdate.update(
				pref.viewpoint(),
				pref.seatHeight(),
				pref.section(),
				pref.seatPositionPref(),
				pref.environmentPref(),
				pref.moodPref(),
				pref.obstructionSensitivity(),
				pref.priceMode(),
				pref.priceMin(),
				pref.priceMax()
			);
		}
	}

	private void validatePreferences(List<OnboardingPreferenceDto> preferences) {
		Preconditions.validate(preferences != null, ErrorCode.ONBOARDING_NOT_COMPLETED);
		Preconditions.validate(preferences
			.size() == 3, ErrorCode.MISSING_REQUIRED_PREFERENCE_FIELD);

		// 우선순위 검증
		validatePriority(preferences);

		// 필수 값 검증
		validateRequiredFields(preferences);

		// 가격 검증
		validatePrice(preferences);
	}

	private void validatePriority(List<OnboardingPreferenceDto> preferences) {
		Set<Integer> priorities = preferences.stream()
			.map(OnboardingPreferenceDto::priority)
			.collect(Collectors.toSet());

		Preconditions.validate(priorities.size() == 3, ErrorCode.INVALID_PREFERENCE_RANK);
		Preconditions.validate(priorities.containsAll(List.of(1, 2, 3)), ErrorCode.INVALID_PREFERENCE_PRIORITY_VALUE);
	}

	private void validateRequiredFields(List<OnboardingPreferenceDto> preferences) {
		Set<Viewpoint> viewpoints = new HashSet<>();
		Set<SeatHeight> seatHeights = new HashSet<>();
		Set<Section> sections = new HashSet<>();

		for (OnboardingPreferenceDto dto : preferences) {
			Preconditions.validate(dto.viewpoint() != null && dto.seatHeight() != null && dto.section() != null,
				ErrorCode.MISSING_REQUIRED_PREFERENCE_FIELD);

			Preconditions.validate(viewpoints.add(dto.viewpoint()), ErrorCode.DUPLICATE_PREFERENCE_VIEWPOINT);
			Preconditions.validate(seatHeights.add(dto.seatHeight()), ErrorCode.DUPLICATE_PREFERENCE_SEAT_HEIGHT);
			Preconditions.validate(sections.add(dto.section()), ErrorCode.DUPLICATE_PREFERENCE_SECTION);
		}
	}

	private void validatePrice(List<OnboardingPreferenceDto> preferences) {
		// TODO: 가격 정책 정해지면 구현
	}

	private OnboardingPreference toEntity(User user, OnboardingPreferenceDto preference) {
		return OnboardingPreference
			.builder()
			.user(user)
			.priority(preference.priority())
			.viewpoint(preference.viewpoint())
			.seatHeight(preference.seatHeight())
			.section(preference.section())
			.seatPositionPref(preference.seatPositionPref())
			.environmentPref(preference.environmentPref())
			.moodPref(preference.moodPref())
			.obstructionSensitivity(preference.obstructionSensitivity())
			.priceMode(preference.priceMode())
			.priceMin(preference.priceMin())
			.priceMax(preference.priceMax())
			.build();
	}

	private void applyMarketingConsent(User user, OnboardingPreferenceCreateRequest.MarketingConsent marketingConsent) {
		Preconditions.validate(marketingConsent != null && marketingConsent.marketingAgreed() != null,
			ErrorCode.INVALID_MARKETING_CONSENT);

		user.updateMarketingConsent(marketingConsent.marketingAgreed());
	}
}
