package com.goormgb.be.seat.block.dto;

import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.seat.block.entity.Block;

public record BlockItemDto(
	Long blockId,
	String blockCode,
	String sectionName,
	String areaName,
	Viewpoint viewpoint
) {

	public static BlockItemDto from(Block block) {
		return new BlockItemDto(
			block.getId(),
			block.getBlockCode(),
			block.getSection().getName(),
			block.getArea().getName(),
			block.getViewpoint()
		);
	}
}
