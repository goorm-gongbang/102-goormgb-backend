package com.goormgb.be.seat.matchSeat.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.seat.enums.SeatZone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "match_seats",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_match_seats_match_id_seat_id",
			columnNames = {"match_id", "seat_id"}
		)
	},
	indexes = {
		// 경기 전체 좌석 조회
		@Index(
			name = "idx_match_seats_match_id",
			columnList = "match_id"
		),

		// 좌석맵 렌더링
		@Index(
			name = "idx_match_seats_match_id_block_id_row_no_seat_no",
			columnList = "match_id, block_id, row_no, seat_no"
		),

		// 연석 탐색
		@Index(
			name = "idx_match_seats_match_id_block_id_row_no_template_col_no",
			columnList = "match_id, block_id, row_no, template_col_no"
		),

		// 예매 가능 좌석 조회
		@Index(
			name = "idx_match_seats_match_id_block_id_sale_status",
			columnList = "match_id, block_id, sale_status"
		),

		// 추천 좌석 조회
		@Index(
			name = "idx_match_seats_match_id_section_id_sale_status_seat_zone",
			columnList = "match_id, section_id, sale_status, seat_zone"
		)
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchSeat extends BaseEntity {

	@Column(name = "match_id", nullable = false)
	private Long matchId;

	@Column(name = "seat_id", nullable = false)
	private Long seatId;

	@Column(name = "area_id", nullable = false)
	private Long areaId;

	@Column(name = "section_id", nullable = false)
	private Long sectionId;

	@Column(name = "block_id", nullable = false)
	private Long blockId;

	@Column(name = "row_no", nullable = false)
	private Integer rowNo;

	@Column(name = "seat_no", nullable = false)
	private Integer seatNo;

	@Column(name = "template_col_no", nullable = false)
	private Integer templateColNo;

	@Enumerated(EnumType.STRING)
	@Column(name = "seat_zone", nullable = false, length = 10)
	private SeatZone seatZone;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_status", nullable = false, length = 20)
	private MatchSeatSaleStatus saleStatus;

	@Builder
	public MatchSeat(
		Long matchId,
		Long seatId,
		Long areaId,
		Long sectionId,
		Long blockId,
		Integer rowNo,
		Integer seatNo,
		Integer templateColNo,
		SeatZone seatZone,
		MatchSeatSaleStatus saleStatus
	) {
		this.matchId = matchId;
		this.seatId = seatId;
		this.areaId = areaId;
		this.sectionId = sectionId;
		this.blockId = blockId;
		this.rowNo = rowNo;
		this.seatNo = seatNo;
		this.templateColNo = templateColNo;
		this.seatZone = seatZone;
		this.saleStatus = saleStatus;
	}

	public void markSold() {
		this.saleStatus = MatchSeatSaleStatus.SOLD;
	}

	public void markBlocked() {
		this.saleStatus = MatchSeatSaleStatus.BLOCKED;
	}

	public void markAvailable() {
		this.saleStatus = MatchSeatSaleStatus.AVAILABLE;
	}
}
