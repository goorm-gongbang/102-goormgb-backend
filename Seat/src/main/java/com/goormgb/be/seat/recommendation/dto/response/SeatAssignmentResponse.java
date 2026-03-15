package com.goormgb.be.seat.recommendation.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;

public record SeatAssignmentResponse(
	Long matchId,
	String blockCode,
	String sectionName,
	List<AssignedSeat> assignedSeats,
	Instant holdExpiresAt,
	boolean semiConsecutive
) {

	public record AssignedSeat(
		Long matchSeatId,
		int rowNo,
		int seatNo,
		int templateColNo
	) {

		public static AssignedSeat from(MatchSeat matchSeat) {
			return new AssignedSeat(
				matchSeat.getId(),
				matchSeat.getRowNo(),
				matchSeat.getSeatNo(),
				matchSeat.getTemplateColNo()
			);
		}
	}

	public static SeatAssignmentResponse of(
		Long matchId,
		Block block,
		List<MatchSeat> assignedSeats,
		Instant holdExpiresAt,
		boolean semiConsecutive
	) {
		List<AssignedSeat> seats = assignedSeats.stream()
			.map(AssignedSeat::from)
			.toList();

		return new SeatAssignmentResponse(
			matchId,
			block.getBlockCode(),
			block.getSection().getName(),
			seats,
			holdExpiresAt,
			semiConsecutive
		);
	}
}
