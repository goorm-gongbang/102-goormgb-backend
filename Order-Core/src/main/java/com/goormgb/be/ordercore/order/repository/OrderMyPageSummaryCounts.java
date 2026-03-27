package com.goormgb.be.ordercore.order.repository;

public record OrderMyPageSummaryCounts(
	long totalCount,
	long upcomingCount,
	long cancelRefundCount,
	long cancelProcessingCount,
	long completedCount
) {
}
