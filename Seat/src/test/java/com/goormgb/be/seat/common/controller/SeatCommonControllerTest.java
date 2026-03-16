package com.goormgb.be.seat.common.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.goormgb.be.seat.common.dto.response.SeatGroupsEntryResponse;
import com.goormgb.be.seat.common.dto.response.SectionBlocksResponse;
import com.goormgb.be.seat.common.service.SeatCommonService;
import com.goormgb.be.seat.support.WebMvcTestSupport;

@WebMvcTest(SeatCommonController.class)
@AutoConfigureMockMvc(addFilters = false)
class SeatCommonControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private SeatCommonService seatCommonService;

	private void setAuthentication(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@Test
	@DisplayName("GET /matches/{matchId}/seat-groups - 좌석 그룹 초기 조회 성공")
	void 좌석_그룹_초기_조회_성공() throws Exception {
		// given
		Long matchId = 10L;
		Long userId = 7L;
		setAuthentication(userId);

		SeatGroupsEntryResponse response = new SeatGroupsEntryResponse(
			new SeatGroupsEntryResponse.MatchInfo(
				matchId,
				new SeatGroupsEntryResponse.ClubInfo(1L, "LG 트윈스", "lg-twins.png"),
				new SeatGroupsEntryResponse.ClubInfo(3L, "두산 베어스", "doosan-bears.png"),
				Instant.parse("2026-04-15T18:30:00Z"),
				new SeatGroupsEntryResponse.StadiumInfo(2L, "잠실 야구장")
			),
			new SeatGroupsEntryResponse.SeatSessionInfo(true, 2),
			List.of(
				new SeatGroupsEntryResponse.SeatGroupInfo(
					11L,
					"프리미엄",
					List.of(
						new SeatGroupsEntryResponse.SectionInfo(101L, "테라존", "테라존", List.of(1001L, 1002L), 25L)
					)
				),
				new SeatGroupsEntryResponse.SeatGroupInfo(
					12L,
					"1루 구역",
					List.of(
						new SeatGroupsEntryResponse.SectionInfo(201L, "내야", "1루 내야", List.of(2001L), 120L)
					)
				)
			)
		);

		given(seatCommonService.getSeatGroupsEntry(eq(matchId), eq(userId))).willReturn(response);

		// when & then
		mockMvc.perform(get("/matches/{matchId}/seat-groups", matchId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.message").value("성공"))
			.andExpect(jsonPath("$.data.match.matchId").value(10))
			.andExpect(jsonPath("$.data.match.homeClub.koName").value("LG 트윈스"))
			.andExpect(jsonPath("$.data.seatSession.recommendationEnabled").value(true))
			.andExpect(jsonPath("$.data.seatSession.ticketCount").value(2))
			.andExpect(jsonPath("$.data.seatGroups.length()").value(2))
			.andExpect(jsonPath("$.data.seatGroups[0].areaName").value("프리미엄"))
			.andExpect(jsonPath("$.data.seatGroups[0].sections[0].sectionId").value(101))
			.andExpect(jsonPath("$.data.seatGroups[0].sections[0].blockIds[0]").value(1001))
			.andExpect(jsonPath("$.data.seatGroups[1].sections[0].displayName").value("1루 내야"))
			.andExpect(jsonPath("$.data.seatGroups[1].sections[0].remainingSeatCount").value(120));

		then(seatCommonService).should().getSeatGroupsEntry(matchId, userId);
	}

	@Test
	@DisplayName("GET /matches/{matchId}/sections/{sectionId}/blocks - 섹션 별 블럭 좌석 현황 조회 성공")
	void 섹션_별_블럭_좌석_현황_조회_성공() throws Exception {
		// given
		Long matchId = 10L;
		Long sectionId = 20L;
		Long userId = 7L;
		setAuthentication(userId);

		SectionBlocksResponse response = new SectionBlocksResponse(
			List.of(
				new SectionBlocksResponse.BlockInfo(
					205L,
					"205",
					"205블럭",
					List.of(
						new SectionBlocksResponse.RowInfo(
							1,
							2,
							List.of(
								new SectionBlocksResponse.SeatInfo(205001L, 1, "AVAILABLE"),
								new SectionBlocksResponse.SeatInfo(205002L, 2, "SOLD"),
								new SectionBlocksResponse.SeatInfo(205003L, 3, "HELD")
							)
						)
					)
				)
			)
		);

		given(seatCommonService.getSectionBlocks(eq(matchId), eq(sectionId), eq(userId))).willReturn(response);

		// when & then
		mockMvc.perform(get("/matches/{matchId}/sections/{sectionId}/blocks", matchId, sectionId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.message").value("성공"))
			.andExpect(jsonPath("$.data.blocks.length()").value(1))
			.andExpect(jsonPath("$.data.blocks[0].blockId").value(205))
			.andExpect(jsonPath("$.data.blocks[0].displayName").value("205블럭"))
			.andExpect(jsonPath("$.data.blocks[0].rows[0].rowNo").value(1))
			.andExpect(jsonPath("$.data.blocks[0].rows[0].remainingSeatCount").value(2))
			.andExpect(jsonPath("$.data.blocks[0].rows[0].seats[2].saleStatus").value("HELD"));

		then(seatCommonService).should().getSectionBlocks(matchId, sectionId, userId);
	}
}