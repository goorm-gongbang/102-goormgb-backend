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
		List<BlockRecommendation> recommendations,
		boolean nearAdjacentToggle
	) {
		List<RecommendedBlock> blocks = new java.util.ArrayList<>();
		for (int i = 0; i < recommendations.size(); i++) {
			blocks.add(RecommendedBlock.from(recommendations.get(i), i + 1, nearAdjacentToggle));
		}
		return new BlockRecommendationResponse(matchId, ticketCount, blocks);
	}

	public record RecommendedBlock(
		Long blockId,
		String blockCode,
		String sectionName,
		String areaName,
		String viewpoint,
		int availableConsecutiveCount,
		long remainingSeatCount,
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
