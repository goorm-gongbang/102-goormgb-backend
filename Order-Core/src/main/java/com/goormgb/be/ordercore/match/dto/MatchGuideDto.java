package com.goormgb.be.ordercore.match.dto;

import com.goormgb.be.ordercore.match.enums.PurchaseStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "경기 안내 정보")
public record MatchGuideDto(
		@Schema(description = "팀 표시 문자열", example = "LG 트윈스 vs 두산 베어스")
		String teamsDisplay,
		@Schema(description = "관람 연령 제한", example = "전체관람가")
		String ageLimit,
		@Schema(description = "경기장 이름", example = "잠실야구장")
		String placeDisplay,
		@Schema(description = "경기장 주소", example = "서울특별시 송파구 올림픽로 25")
		String addressDisplay,
		@Schema(description = "경기 날짜/시간 표시 (KST)", example = "2026년 03월 28일 (토) 14:00")
		String datetimeDisplay,
		@Schema(description = "구매 가능 여부", example = "PURCHASABLE")
		PurchaseStatus purchaseStatus,
		@Schema(description = "D-Day 라벨", example = "D-18")
		String matchDdayLabel
) {
}