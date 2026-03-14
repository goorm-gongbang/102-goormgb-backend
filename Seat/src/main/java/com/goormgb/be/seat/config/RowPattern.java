package com.goormgb.be.seat.config;

public record RowPattern(
	int rowNo,
	int seatCount,
	int startTemplateColNo
) {
}
