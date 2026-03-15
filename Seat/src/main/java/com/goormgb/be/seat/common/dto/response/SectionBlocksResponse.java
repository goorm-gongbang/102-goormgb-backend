package com.goormgb.be.seat.common.dto.response;

import java.util.List;

public record SectionBlocksResponse(
	List<BlockInfo> blocks
) {

	public record BlockInfo(
		Long blockId,
		String blockCode,
		String displayName,
		List<RowInfo> rows
	) {
	}

	public record RowInfo(
		int rowNo,
		long remainingSeatCount,
		List<SeatInfo> seats
	) {
	}

	public record SeatInfo(
		Long seatId,
		int seatNo,
		String saleStatus
	) {
	}
}