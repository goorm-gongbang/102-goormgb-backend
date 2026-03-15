package com.goormgb.be.seat.common.dto.response;

import java.util.List;

import com.goormgb.be.seat.area.entity.Area;
import com.goormgb.be.seat.section.entity.Section;

public record SeatGroupResponse(
	Long areaId,
	String areaName,
	List<SectionInfo> sections
) {

	public static SeatGroupResponse of(Area area, List<SectionInfo> sections) {
		return new SeatGroupResponse(
			area.getId(),
			area.getName(),
			sections
		);
	}

	public record SectionInfo(
		Long sectionId,
		String sectionName,
		String displayName,
		List<String> blockCodes,
		long remainingSeatCount
	) {

		public static SectionInfo of(
			Section section,
			List<String> blockCodes,
			long remainingSeatCount
		) {
			return new SectionInfo(
				section.getId(),
				section.getName(),
				section.getName(),
				blockCodes,
				remainingSeatCount
			);
		}
	}
}