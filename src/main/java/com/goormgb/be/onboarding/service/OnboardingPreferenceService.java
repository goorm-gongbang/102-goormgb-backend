package com.goormgb.be.onboarding.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.onboarding.dto.request.OnboardingPreferenceCreateRequest;
import com.goormgb.be.onboarding.dto.response.OnboardingPreferenceCreateResponse;
import com.goormgb.be.onboarding.entity.OnboardingPreference;
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

	public OnboardingPreferenceCreateResponse createPreferences(Long userId, OnboardingPreferenceCreateRequest request) {
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);

		// 검증
		validateRequest(request);

		// 저장
		List<OnboardingPreference> entities = request
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

	private void validateRequest(OnboardingPreferenceCreateRequest request) {
		Preconditions.validate(request != null && request.preferences() != null, ErrorCode.BAD_REQUEST);
		Preconditions.validate(request
			.preferences()
			.size() == 3, ErrorCode.BAD_REQUEST);

		// TODO: 우선순위, 필수 값 중복 검증
		// TODO: 가격 검증
	}

	private OnboardingPreference toEntity(User user, OnboardingPreferenceCreateRequest.Preference preference) {
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
			ErrorCode.BAD_REQUEST);

		user.updateMarketingConsent(marketingConsent.marketingAgreed());
	}
}
