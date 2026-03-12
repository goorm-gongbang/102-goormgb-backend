package com.goormgb.be.seat.block.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.goormgb.be.seat.block.service.BlockService;
import com.goormgb.be.seat.fixture.BlockFixture;
import com.goormgb.be.seat.support.WebMvcTestSupport;

@WebMvcTest(controllers = BlockController.class)
@AutoConfigureMockMvc(addFilters = false)
class BlockControllerTest extends WebMvcTestSupport {

	@MockitoBean
	private BlockService blockService;

	@Test
	@DisplayName("GET /blocks - 전체 블럭 목록 조회 성공")
	void 전체_블럭_목록_조회_성공() throws Exception {
		// given
		given(blockService.getAllBlocks()).willReturn(BlockFixture.blockListResponse());

		// when & then
		mockMvc.perform(get("/blocks"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.message").value("성공"))
			.andExpect(jsonPath("$.data.blocks").isArray())
			.andExpect(jsonPath("$.data.blocks.length()").value(3))
			.andExpect(jsonPath("$.data.blocks[0].blockCode").value("CP"))
			.andExpect(jsonPath("$.data.blocks[0].sectionName").value("테라존(중앙 프리미엄석)"))
			.andExpect(jsonPath("$.data.blocks[0].sectionColor").value("#D4AF37"))
			.andExpect(jsonPath("$.data.blocks[0].areaName").value("중앙"))
			.andExpect(jsonPath("$.data.blocks[0].viewpoint").value("CENTER"));
	}

	@Test
	@DisplayName("GET /blocks - 블럭이 없으면 빈 배열을 반환한다")
	void 전체_블럭_목록_없음() throws Exception {
		// given
		given(blockService.getAllBlocks()).willReturn(BlockFixture.emptyBlockListResponse());

		// when & then
		mockMvc.perform(get("/blocks"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("OK"))
			.andExpect(jsonPath("$.data.blocks").isArray())
			.andExpect(jsonPath("$.data.blocks.length()").value(0));
	}
}
