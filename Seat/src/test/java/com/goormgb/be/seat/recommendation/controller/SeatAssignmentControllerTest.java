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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.goormgb.be.seat.recommendation.dto.response.SeatAssignmentResponse;
import com.goormgb.be.seat.recommendation.service.SeatAssignmentService;
import com.goormgb.be.seat.recommendation.service.SeatRecommendationService;

@WebMvcTest(SeatRecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class SeatAssignmentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SeatRecommendationService seatRecommendationService;

	@MockitoBean
	private SeatAssignmentService seatAssignmentService;

	private void setAuthentication(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	@DisplayName("좌석 배정 및 선점 요청에 성공한다")
	void 좌석_배정_성공() throws Exception {
		// given
		Long matchId = 1L;
		Long blockId = 10L;
		Long userId = 1L;
		setAuthentication(userId);

		Instant expiresAt = Instant.parse("2026-04-15T10:05:00Z");
		SeatAssignmentResponse response = new SeatAssignmentResponse(
			matchId,
			"CP",
			"테라존(중앙 프리미엄석)",
			List.of(
				new SeatAssignmentResponse.AssignedSeat(101L, 1, 1, 1),
				new SeatAssignmentResponse.AssignedSeat(102L, 1, 2, 2),
				new SeatAssignmentResponse.AssignedSeat(103L, 1, 3, 3)
			),
			expiresAt,
			false
		);

		given(seatAssignmentService.assignAndHoldSeats(eq(userId), eq(matchId), eq(blockId), eq(false)))
			.willReturn(response);

		// when & then
		mockMvc.perform(post("/matches/{matchId}/recommendations/blocks/{blockId}/assign", matchId, blockId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nearAdjacentToggle\": false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.data.matchId").value(1))
			.andExpect(jsonPath("$.data.blockCode").value("CP"))
			.andExpect(jsonPath("$.data.sectionName").value("테라존(중앙 프리미엄석)"))
			.andExpect(jsonPath("$.data.assignedSeats").isArray())
			.andExpect(jsonPath("$.data.assignedSeats.length()").value(3))
			.andExpect(jsonPath("$.data.assignedSeats[0].rowNo").value(1))
			.andExpect(jsonPath("$.data.assignedSeats[0].seatNo").value(1))
			.andExpect(jsonPath("$.data.semiConsecutive").value(false));
	}
}
