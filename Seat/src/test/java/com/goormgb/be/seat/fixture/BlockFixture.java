package com.goormgb.be.seat.fixture;

import java.util.List;

import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.seat.area.entity.Area;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.block.dto.BlockItemDto;
import com.goormgb.be.seat.block.dto.BlockListResponse;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.enums.SectionCode;

public final class BlockFixture {

	private BlockFixture() {
	}

	public static Area centerArea() {
		return Area.builder()
			.code(AreaCode.CENTER)
			.name("중앙")
			.build();
	}

	public static Area homeArea() {
		return Area.builder()
			.code(AreaCode.HOME)
			.name("1루(홈)")
			.build();
	}

	public static Area outfieldArea() {
		return Area.builder()
			.code(AreaCode.OUTFIELD)
			.name("외야")
			.build();
	}

	public static Section premiumSection(Area area) {
		return Section.builder()
			.area(area)
			.code(SectionCode.PREMIUM)
			.name("테라존(중앙 프리미엄석)")
			.build();
	}

	public static Section orangeSection(Area area) {
		return Section.builder()
			.area(area)
			.code(SectionCode.ORANGE)
			.name("오렌지석")
			.build();
	}

	public static Section greenSection(Area area) {
		return Section.builder()
			.area(area)
			.code(SectionCode.GREEN)
			.name("그린석(외야석)")
			.build();
	}

	public static Block cpBlock() {
		Area area = centerArea();
		Section section = premiumSection(area);
		return Block.builder()
			.area(area)
			.section(section)
			.blockCode("CP")
			.viewpoint(Viewpoint.CENTER)
			.homeCheerRank(50)
			.awayCheerRank(50)
			.build();
	}

	public static Block homeOrangeBlock() {
		Area area = homeArea();
		Section section = orangeSection(area);
		return Block.builder()
			.area(area)
			.section(section)
			.blockCode("205")
			.viewpoint(Viewpoint.INFIELD_1B)
			.homeCheerRank(1)
			.awayCheerRank(81)
			.build();
	}

	public static Block outfieldBlock() {
		Area area = outfieldArea();
		Section section = greenSection(area);
		return Block.builder()
			.area(area)
			.section(section)
			.blockCode("408")
			.viewpoint(Viewpoint.OUTFIELD_C)
			.homeCheerRank(70)
			.awayCheerRank(70)
			.build();
	}

	public static List<Block> allBlocks() {
		return List.of(cpBlock(), homeOrangeBlock(), outfieldBlock());
	}

	public static BlockListResponse blockListResponse() {
		return BlockListResponse.from(allBlocks());
	}

	public static BlockListResponse emptyBlockListResponse() {
		return new BlockListResponse(List.of());
	}

	public static BlockItemDto cpBlockItemDto() {
		return new BlockItemDto(null, "CP", "테라존(중앙 프리미엄석)", "중앙", Viewpoint.CENTER);
	}
}
