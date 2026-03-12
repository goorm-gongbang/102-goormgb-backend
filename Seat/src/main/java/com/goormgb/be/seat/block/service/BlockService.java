package com.goormgb.be.seat.block.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.seat.block.dto.BlockListResponse;
import com.goormgb.be.seat.block.repository.BlockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlockService {

	private final BlockRepository blockRepository;

	@Transactional(readOnly = true)
	public BlockListResponse getAllBlocks() {
		var blocks = blockRepository.findAllWithSectionAndArea();
		return BlockListResponse.from(blocks);
	}
}
