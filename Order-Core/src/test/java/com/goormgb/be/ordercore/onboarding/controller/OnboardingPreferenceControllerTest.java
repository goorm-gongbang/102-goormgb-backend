package com.goormgb.be.ordercore.onboarding.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.goormgb.be.domain.onboarding.enums.CheerProximityPref;
import com.goormgb.be.domain.onboarding.enums.EnvironmentPref;
import com.goormgb.be.domain.onboarding.enums.MoodPref;
import com.goormgb.be.domain.onboarding.enums.ObstructionSensitivity;
import com.goormgb.be.domain.onboarding.enums.PriceMode;
import com.goormgb.be.domain.onboarding.enums.SeatHeight;
import com.goormgb.be.domain.onboarding.enums.SeatPositionPref;
import com.goormgb.be.domain.onboarding.enums.Section;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.fixture.onboarding.OnboardingPreferenceRequestFixture;
import com.goormgb.be.ordercore.onboarding.dto.OnboardingPreferenceItemDto;
import com.goormgb.be.ordercore.onboarding.dto.request.OnboardingPreferenceCreateRequest;
import com.goormgb.be.ordercore.onboarding.dto.request.OnboardingPreferenceUpdateRequest;
import com.goormgb.be.ordercore.onboarding.dto.response.OnboardingPreferenceCreateResponse;
import com.goormgb.be.ordercore.onboarding.dto.response.OnboardingPreferenceGetResponse;
import com.goormgb.be.ordercore.onboarding.service.OnboardingPreferenceService;
import com.goormgb.be.ordercore.support.WebMvcTestSupport;

@WebMvcTest(controllers = OnboardingPreferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class OnboardingPreferenceControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private OnboardingPreferenceService onboardingPreferenceService;

	private void setAuthentication(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	@DisplayName("GET /onboarding/preferences - 선호도 조회 성공")
	void 선호도_조회_성공() throws Exception {
		// given
		Long userId = 1L;
		setAuthentication(userId);

		OnboardingPreferenceGetResponse response = new OnboardingPreferenceGetResponse(
			1L,
			"LG 트윈스",
			CheerProximityPref.NEAR,
			List.of(1L, 5L, 12L),
			List.of(
				new OnboardingPreferenceItemDto(
					1, Viewpoint.CENTER,
					SeatHeight.LOW, Section.CENTER_SIDE,
					SeatPositionPref.AISLE, EnvironmentPref.SHADE,
					MoodPref.CHEERFUL, ObstructionSensitivity.NORMAL,
					PriceMode.RANGE, 30000, 80000
				),
				new OnboardingPreferenceItemDto(
					2, Viewpoint.INFIELD_1B,
					SeatHeight.LOW, Section.CENTER_SIDE,
					SeatPositionPref.AISLE, EnvironmentPref.SHADE,
					MoodPref.CHEERFUL, ObstructionSensitivity.NORMAL,
					PriceMode.RANGE, 30000, 80000
				)
			)
		);

		given(onboardingPreferenceService.getPreferences(userId)).willReturn(response);

		// when & then
		mockMvc.perform(get("/onboarding/preferences"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.message").value("성공"))
			.andExpect(jsonPath("$.data.favoriteClubId").value(1))
			.andExpect(jsonPath("$.data.cheerProximityPref").value("NEAR"))
			.andExpect(jsonPath("$.data.preferredBlockIds").isArray())
			.andExpect(jsonPath("$.data.preferredBlockIds.length()").value(3))
			.andExpect(jsonPath("$.data.preferences").isArray())
			.andExpect(jsonPath("$.data.preferences.length()").value(2))
			.andExpect(jsonPath("$.data.preferences[0].priority").value(1))
			.andExpect(jsonPath("$.data.preferences[0].viewpoint").value("CENTER"));
	}

	@Test
	@DisplayName("GET /onboarding/preferences - 존재하지 않는 사용자 404")
	void 선호도_조회_존재하지않는_사용자() throws Exception {
		// given
		Long userId = 999L;
		setAuthentication(userId);

		given(onboardingPreferenceService.getPreferences(userId))
			.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

		// when & then
		mockMvc.perform(get("/onboarding/preferences"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("POST /onboarding/preferences - 선호도 생성 성공")
	void 선호도_생성_성공() throws Exception {
		// given
		Long userId = 1L;
		setAuthentication(userId);

		OnboardingPreferenceCreateRequest request = OnboardingPreferenceRequestFixture.createCreateRequest();

		Instant now = LocalDateTime.of(2026, 2, 22, 12, 0, 0).toInstant(ZoneOffset.UTC);
		OnboardingPreferenceCreateResponse response = new OnboardingPreferenceCreateResponse(
			true, now, true, now
		);

		given(onboardingPreferenceService.createPreferences(eq(userId), any(OnboardingPreferenceCreateRequest.class)))
			.willReturn(response);

		// when & then
		mockMvc.perform(post("/onboarding/preferences")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.message").value("성공"))
			.andExpect(jsonPath("$.data.onboardingStatus").value(true))
			.andExpect(jsonPath("$.data.marketingConsent").value(true))
			.andExpect(jsonPath("$.data.onboardingCompletedAt").exists())
			.andExpect(jsonPath("$.data.marketingConsentedAt").exists());
	}

	@Test
	@DisplayName("POST /onboarding/preferences - 이미 온보딩이 완료된 경우 409")
	void 선호도_생성_이미_온보딩_완료() throws Exception {
		// given
		Long userId = 1L;
		setAuthentication(userId);

		OnboardingPreferenceCreateRequest request = OnboardingPreferenceRequestFixture.createCreateRequest();

		given(onboardingPreferenceService.createPreferences(eq(userId), any(OnboardingPreferenceCreateRequest.class)))
			.willThrow(new CustomException(ErrorCode.ONBOARDING_ALREADY_COMPLETED));

		// when & then
		mockMvc.perform(post("/onboarding/preferences")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 온보딩이 완료되었습니다."));
	}

	@Test
	@DisplayName("POST /onboarding/preferences - 존재하지 않는 사용자 404")
	void 선호도_생성_존재하지않는_사용자() throws Exception {
		// given
		Long userId = 999L;
		setAuthentication(userId);

		OnboardingPreferenceCreateRequest request = OnboardingPreferenceRequestFixture.createCreateRequest();

		given(onboardingPreferenceService.createPreferences(eq(userId), any(OnboardingPreferenceCreateRequest.class)))
			.willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/onboarding/preferences")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("PUT /onboarding/preferences - 선호도 수정 성공")
	void 선호도_수정_성공() throws Exception {
		// given
		Long userId = 1L;
		setAuthentication(userId);

		OnboardingPreferenceUpdateRequest request = OnboardingPreferenceRequestFixture.createUpdateRequest();

		willDoNothing().given(onboardingPreferenceService)
			.updatePreferences(eq(userId), any(OnboardingPreferenceUpdateRequest.class));

		// when & then
		mockMvc.perform(put("/onboarding/preferences")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.message").value("성공"));
	}

	@Test
	@DisplayName("PUT /onboarding/preferences - 존재하지 않는 사용자 404")
	void 선호도_수정_존재하지않는_사용자() throws Exception {
		// given
		Long userId = 999L;
		setAuthentication(userId);

		OnboardingPreferenceUpdateRequest request = OnboardingPreferenceRequestFixture.createUpdateRequest();

		willThrow(new CustomException(ErrorCode.USER_NOT_FOUND))
			.given(onboardingPreferenceService)
			.updatePreferences(eq(userId), any(OnboardingPreferenceUpdateRequest.class));

		// when & then
		mockMvc.perform(put("/onboarding/preferences")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
	}
}
