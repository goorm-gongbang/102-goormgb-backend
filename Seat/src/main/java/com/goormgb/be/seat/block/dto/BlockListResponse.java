package com.goormgb.be.seat.block.dto;

import java.util.List;

import com.goormgb.be.seat.block.entity.Block;

public record BlockListResponse(
	List<BlockItemDto> blocks
) {

	public static BlockListResponse from(List<Block> blocks) {
		return new BlockListResponse(
			blocks.stream().map(BlockItemDto::from).toList()
		);
	}
}
