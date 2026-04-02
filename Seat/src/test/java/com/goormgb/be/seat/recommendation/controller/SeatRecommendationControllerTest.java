package com.goormgb.be.seat.recommendation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.goormgb.be.seat.recommendation.dto.response.SeatEntryResponse;
import com.goormgb.be.global.environment.ErrorResponseStrategy;
import com.goormgb.be.global.security.filter.XUserIdAuthenticationFilter;
import com.goormgb.be.seat.recommendation.service.SeatAssignmentService;
import com.goormgb.be.seat.recommendation.service.SeatRecommendationService;
import com.goormgb.be.seat.security.AdmissionTokenValidator;

@WebMvcTest(SeatRecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class SeatRecommendationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SeatRecommendationService seatRecommendationService;

	@MockitoBean
	private SeatAssignmentService seatAssignmentService;

	@MockitoBean
	private AdmissionTokenValidator admissionTokenValidator;

	@MockitoBean
	private ErrorResponseStrategy errorResponseStrategy;

	@MockitoBean
	private XUserIdAuthenticationFilter xUserIdAuthenticationFilter;

	private void setAuthentication(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	@DisplayName("추천 좌석 초기 조회에 성공한다")
	void 추천_좌석_초기_조회_성공() throws Exception {
		// given
		Long matchId = 10L;
		Long userId = 1L;
		setAuthentication(userId);

		SeatEntryResponse response = new SeatEntryResponse(
			new SeatEntryResponse.MatchInfo(
				10L,
				new SeatEntryResponse.ClubInfo(1L, "LG 트윈스", "lg-twins.png"),
				new SeatEntryResponse.ClubInfo(3L, "두산 베어스", "doosan-bears.png"),
				Instant.parse("2026-04-15T18:30:00Z"),
				new SeatEntryResponse.StadiumInfo(3L, "잠실 야구장")
			),
			new SeatEntryResponse.SeatSessionInfo(
				true,
				2,
				false
			)
		);

		given(seatRecommendationService.getRecommendationSeatEntry(eq(matchId), eq(userId)))
			.willReturn(response);

		// when & then
		mockMvc.perform(get("/matches/{matchId}/recommendations/seat-entry", matchId)
				.cookie(new jakarta.servlet.http.Cookie("admissionToken", "test-token")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.data.match.matchId").value(10))
			.andExpect(jsonPath("$.data.match.homeClub.clubId").value(1))
			.andExpect(jsonPath("$.data.match.homeClub.koName").value("LG 트윈스"))
			.andExpect(jsonPath("$.data.match.awayClub.clubId").value(3))
			.andExpect(jsonPath("$.data.match.awayClub.koName").value("두산 베어스"))
			.andExpect(jsonPath("$.data.match.stadium.stadiumId").value(3))
			.andExpect(jsonPath("$.data.match.stadium.koName").value("잠실 야구장"))
			.andExpect(jsonPath("$.data.seatSession.recommendationEnabled").value(true))
			.andExpect(jsonPath("$.data.seatSession.ticketCount").value(2));
	}
}
