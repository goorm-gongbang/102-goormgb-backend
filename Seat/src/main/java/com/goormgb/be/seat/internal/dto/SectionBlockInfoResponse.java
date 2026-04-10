package com.goormgb.be.seat.internal.dto;

public record SectionBlockInfoResponse(
	Long sectionId,
	String sectionName,
	Long blockId,
	String blockCode
) {
}
