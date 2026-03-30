package com.goormgb.be.seat.recommendation.dto.response;

import java.util.List;

import com.goormgb.be.seat.recommendation.dto.internal.BlockRecommendation;

public record BlockRecommendationResponse(
	Long matchId,
	Integer ticketCount,
	List<RecommendedBlock> blocks
) {

	public static BlockRecommendationResponse of(
		Long matchId,
		Integer ticketCount,
		List<BlockRecommendation> recommendations
	) {
		List<RecommendedBlock> blocks = new java.util.ArrayList<>();
		for (int i = 0; i < recommendations.size(); i++) {
			blocks.add(RecommendedBlock.from(recommendations.get(i), i + 1));
		}
		return new BlockRecommendationResponse(matchId, ticketCount, blocks);
	}

	public record RecommendedBlock(
		Long blockId,
		String blockCode,
		String sectionName,
		String areaName,
		String viewpoint,
		int realConsecutiveCount,
		int semiConsecutiveCount,
		long remainingSeatCount,
		int rank
	) {

		public static RecommendedBlock from(BlockRecommendation recommendation, int rank) {
			var block = recommendation.block();
			return new RecommendedBlock(
				block.getBlockNum(),
				block.getBlockCode(),
				block.getSection().getName(),
				block.getArea().getName(),
				block.getViewpoint().name(),
				recommendation.realConsecutiveCount(),
				recommendation.semiConsecutiveCount(),
				recommendation.remainingSeatCount(),
				rank
			);
		}
	}
}
