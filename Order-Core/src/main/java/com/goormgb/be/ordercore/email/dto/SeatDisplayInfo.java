package com.goormgb.be.ordercore.email.dto;

public record SeatDisplayInfo(
	String sectionName,
	Long blockNum,
	Integer rowNo,
	Integer seatNo,
	Integer price
) {

	public String displayName() {
		return String.format("%s %d블럭 %d열 %d번", sectionName, blockNum, rowNo, seatNo);
	}
}
