package com.goormgb.be.seat.common.dto.response;

import java.util.List;

public record SectionBlocksResponse(
	List<BlockInfo> blocks
) {

	public static SectionBlocksResponse of(List<BlockInfo> blocks) {
		return new SectionBlocksResponse(blocks);
	}

	public record BlockInfo(
		Long blockId,
		String blockCode,
		String displayName,
		List<RowInfo> rows
	) {
		public static BlockInfo of(Long blockId, String blockCode, List<RowInfo> rows) {
			return new BlockInfo(
				blockId,
				blockCode,
				blockCode + "블럭",
				rows
			);
		}
	}

	public record RowInfo(
		int rowNo,
		long remainingSeatCount,
		List<SeatInfo> seats
	) {
		public static RowInfo of(int rowNo, long remainingSeatCount, List<SeatInfo> seats) {
			return new RowInfo(rowNo, remainingSeatCount, seats);
		}
	}

	public record SeatInfo(
		Long seatId,
		int seatNo,
		String saleStatus
	) {
		public static SeatInfo of(Long seatId, int seatNo, String saleStatus) {
			return new SeatInfo(seatId, seatNo, saleStatus);
		}
	}
}