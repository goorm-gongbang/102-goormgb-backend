package com.goormgb.be.seat.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.seat.area.entity.Area;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.area.repository.AreaRepository;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.seat.repository.SeatRepository;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.enums.SectionCode;
import com.goormgb.be.seat.section.repository.SectionRepository;

@ExtendWith(MockitoExtension.class)
class SeatDataInitializerTest {

	@Mock
	private AreaRepository areaRepository;

	@Mock
	private SectionRepository sectionRepository;

	@Mock
	private BlockRepository blockRepository;

	@Mock
	private SeatRepository seatRepository;

	@InjectMocks
	private SeatDataInitializer seatDataInitializer;

	@Test
	@DisplayName("블럭 데이터가 이미 존재하면 시드 작업을 스킵한다")
	void 블럭_데이터_존재시_시드_스킵() throws Exception {
		// given
		given(blockRepository.count()).willReturn(1L);

		// when
		seatDataInitializer.run();

		// then
		then(areaRepository).shouldHaveNoInteractions();
		then(sectionRepository).shouldHaveNoInteractions();
		then(blockRepository).should(never()).save(any(Block.class));
	}

	@Test
	@DisplayName("블럭 데이터가 없으면 잠실 좌석 구조를 저장한다")
	void 블럭_데이터_없으면_시드_저장() throws Exception {
		// given
		given(blockRepository.count()).willReturn(0L, 107L);
		given(areaRepository.save(any(Area.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(sectionRepository.save(any(Section.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(blockRepository.save(any(Block.class))).willAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<Area> areaCaptor = ArgumentCaptor.forClass(Area.class);
		ArgumentCaptor<Section> sectionCaptor = ArgumentCaptor.forClass(Section.class);
		ArgumentCaptor<Block> blockCaptor = ArgumentCaptor.forClass(Block.class);

		// when
		seatDataInitializer.run();

		// then
		then(areaRepository).should(times(4)).save(areaCaptor.capture());
		then(sectionRepository).should(times(14)).save(sectionCaptor.capture());
		then(blockRepository).should(times(107)).save(blockCaptor.capture());
		then(blockRepository).should(times(2)).count();

		assertThat(areaCaptor.getAllValues())
			.extracting(Area::getCode)
			.containsExactly(AreaCode.HOME, AreaCode.AWAY, AreaCode.OUTFIELD, AreaCode.CENTER);

		assertThat(sectionCaptor.getAllValues())
			.extracting(Section::getCode)
			.contains(SectionCode.PREMIUM, SectionCode.GREEN, SectionCode.ORANGE, SectionCode.NAVY);

		List<Block> savedBlocks = blockCaptor.getAllValues();
		assertThat(savedBlocks)
			.extracting(Block::getBlockCode)
			.contains("CP", "205", "219", "408", "422");

		Block premiumBlock = savedBlocks.stream()
			.filter(block -> "CP".equals(block.getBlockCode()))
			.findFirst()
			.orElseThrow();
		assertThat(premiumBlock.getArea().getCode()).isEqualTo(AreaCode.CENTER);
		assertThat(premiumBlock.getSection().getCode()).isEqualTo(SectionCode.PREMIUM);
		assertThat(premiumBlock.getViewpoint()).isEqualTo(Viewpoint.CENTER);
		assertThat(premiumBlock.getHomeCheerRank()).isEqualTo(50);
		assertThat(premiumBlock.getAwayCheerRank()).isEqualTo(50);

		Block homeCheerBlock = savedBlocks.stream()
			.filter(block -> "205".equals(block.getBlockCode()))
			.findFirst()
			.orElseThrow();
		assertThat(homeCheerBlock.getArea().getCode()).isEqualTo(AreaCode.HOME);
		assertThat(homeCheerBlock.getSection().getCode()).isEqualTo(SectionCode.ORANGE);
		assertThat(homeCheerBlock.getViewpoint()).isEqualTo(Viewpoint.INFIELD_1B);
		assertThat(homeCheerBlock.getHomeCheerRank()).isEqualTo(1);
		assertThat(homeCheerBlock.getAwayCheerRank()).isEqualTo(80);

		Block outfieldCenterBlock = savedBlocks.stream()
			.filter(block -> "408".equals(block.getBlockCode()))
			.findFirst()
			.orElseThrow();
		assertThat(outfieldCenterBlock.getArea().getCode()).isEqualTo(AreaCode.OUTFIELD);
		assertThat(outfieldCenterBlock.getSection().getCode()).isEqualTo(SectionCode.GREEN);
		assertThat(outfieldCenterBlock.getViewpoint()).isEqualTo(Viewpoint.OUTFIELD_C);
		assertThat(outfieldCenterBlock.getHomeCheerRank()).isEqualTo(70);
		assertThat(outfieldCenterBlock.getAwayCheerRank()).isEqualTo(70);
	}
}
