package com.goormgb.be.seat.seat.dto;

import com.goormgb.be.seat.seat.enums.SeatZone;

public interface SeatTemplateProjection {
	Long getSeatId();

	Long getAreaId();

	Long getSectionId();

	Long getBlockId();

	Integer getRowNo();

	Integer getSeatNo();

	Integer getTemplateColNo();

	SeatZone getSeatZone();
}
