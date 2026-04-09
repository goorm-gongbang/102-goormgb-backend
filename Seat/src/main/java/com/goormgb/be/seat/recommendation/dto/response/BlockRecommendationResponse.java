package com.goormgb.be.seat.recommendation.dto.response;

import java.util.List;

import com.goormgb.be.seat.recommendation.dto.internal.BlockRecommendation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "추천 블럭 리스트 응답")
public record BlockRecommendationResponse(
	@Schema(description = "경기 ID", example = "1")
	Long matchId,
	@Schema(description = "티켓 수량 (N연석 기준)", example = "2")
	Integer ticketCount,
	@Schema(description = "추천 블럭 리스트 (추천 순위 정렬)")
	List<RecommendedBlock> blocks
) {

	public static BlockRecommendationResponse of(
		Long matchId,
		Integer ticketCount,
		List<BlockRecommendation> recommendations,
		boolean nearAdjacentToggle
	) {
		List<RecommendedBlock> blocks = new java.util.ArrayList<>();
		for (int i = 0; i < recommendations.size(); i++) {
			blocks.add(RecommendedBlock.from(recommendations.get(i), i + 1, nearAdjacentToggle));
		}
		return new BlockRecommendationResponse(matchId, ticketCount, blocks);
	}

	@Schema(description = "추천 블럭 정보")
	public record RecommendedBlock(
		@Schema(description = "블럭 ID", example = "205")
		Long blockId,
		@Schema(description = "블럭 코드", example = "205")
		String blockCode,
		@Schema(description = "구역 이름", example = "오렌지석(응원석)")
		String sectionName,
		@Schema(description = "영역 이름", example = "1루 외야")
		String areaName,
		@Schema(description = "시야 타입", example = "HOME_BEHIND")
		String viewpoint,
		@Schema(description = "이용 가능 연석 조합 수 (준연석 토글 ON: 찐연석+준연석 합산 / OFF: 찐연석만)", example = "5")
		int availableConsecutiveCount,
		@Schema(description = "잔여 좌석 수", example = "132")
		long remainingSeatCount,
		@Schema(description = "추천 순위 (1부터 시작)", example = "1")
		int rank
	) {

		public static RecommendedBlock from(
			BlockRecommendation recommendation, int rank, boolean nearAdjacentToggle
		) {
			var block = recommendation.block();
			int availableCount = nearAdjacentToggle
				? recommendation.combinedCount()
				: recommendation.realConsecutiveCount();
			return new RecommendedBlock(
				block.getBlockNum(),
				block.getBlockCode(),
				block.getSection().getName(),
				block.getArea().getName(),
				block.getViewpoint().name(),
				availableCount,
				recommendation.remainingSeatCount(),
				rank
			);
		}
	}
}
