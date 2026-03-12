package com.goormgb.be.seat.block.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.seat.block.dto.BlockListResponse;
import com.goormgb.be.seat.block.service.BlockService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Block", description = "경기장 블럭 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/blocks")
public class BlockController {

	private final BlockService blockService;

	@Operation(
		summary = "전체 블럭 목록 조회",
		description = """
			경기장의 전체 블럭 목록을 조회합니다.
			온보딩 시 선호 블럭(preferredBlockIds) 선택에 사용됩니다.
			- 내야(Infield): 101 ~ 334번대 구역 및 특수석
			- 외야(Outfield): 401 ~ 422번대 구역 (그린석)
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요")
	})
	@GetMapping
	public ApiResult<BlockListResponse> getAllBlocks() {
		return ApiResult.ok(blockService.getAllBlocks());
	}
}
