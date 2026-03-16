package com.goormgb.be.seat.block.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.seat.block.dto.BlockListResponse;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.fixture.BlockFixture;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

	@Mock
	private BlockRepository blockRepository;

	@InjectMocks
	private BlockService blockService;

	@Test
	@DisplayName("전체 블럭 목록 조회 시 엔티티를 응답 DTO로 변환한다")
	void 전체_블럭_목록_조회_성공() {
		// given
		given(blockRepository.findAllWithSectionAndArea()).willReturn(BlockFixture.allBlocks());

		// when
		BlockListResponse result = blockService.getAllBlocks();

		// then
		assertThat(result.blocks()).hasSize(3);
		assertThat(result.blocks().get(0).blockCode()).isEqualTo("CP");
		assertThat(result.blocks().get(0).sectionName()).isEqualTo("테라존(중앙 프리미엄석)");
		assertThat(result.blocks().get(0).areaName()).isEqualTo("중앙");
		assertThat(result.blocks().get(0).viewpoint()).isEqualTo(BlockFixture.cpBlock().getViewpoint());
		then(blockRepository).should().findAllWithSectionAndArea();
	}

	@Test
	@DisplayName("전체 블럭이 없으면 빈 응답을 반환한다")
	void 전체_블럭_목록_없음() {
		// given
		given(blockRepository.findAllWithSectionAndArea()).willReturn(java.util.List.of());

		// when
		BlockListResponse result = blockService.getAllBlocks();

		// then
		assertThat(result.blocks()).isEmpty();
		then(blockRepository).should().findAllWithSectionAndArea();
	}
}
