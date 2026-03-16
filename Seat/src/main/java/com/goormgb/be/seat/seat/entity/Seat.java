package com.goormgb.be.seat.seat.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.seat.enums.SeatZone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "seats",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_seat_block_row_seat",
			columnNames = {"block_id", "row_no", "seat_no"}
		),
		@UniqueConstraint(
			name = "uk_seat_block_row_template_col",
			columnNames = {"block_id", "row_no", "template_col_no"}
		)
	},
	indexes = {
		@Index(name = "idx_seat_block_id", columnList = "block_id"),
		@Index(name = "idx_seat_block_seat_zone", columnList = "block_id, seat_zone")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "block_id", nullable = false)
	private Block block;

	@Column(name = "row_no", nullable = false)
	private Integer rowNo;

	@Column(name = "seat_no", nullable = false)
	private Integer seatNo;

	@Column(name = "template_col_no", nullable = false)
	private Integer templateColNo;

	@Enumerated(EnumType.STRING)
	@Column(name = "seat_zone", nullable = false, length = 10)
	private SeatZone seatZone;

	@Builder
	public Seat(
		Block block,
		Integer rowNo,
		Integer seatNo,
		Integer templateColNo,
		SeatZone seatZone
	) {
		this.block = block;
		this.rowNo = rowNo;
		this.seatNo = seatNo;
		this.templateColNo = templateColNo;
		this.seatZone = seatZone;
	}
}