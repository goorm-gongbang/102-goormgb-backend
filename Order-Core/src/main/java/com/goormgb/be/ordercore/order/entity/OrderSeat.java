package com.goormgb.be.ordercore.order.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.domain.ticket.enums.TicketType;

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
	name = "order_seats",
	indexes = {
		@Index(name = "idx_order_seats_order_id", columnList = "order_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_order_seats_match_seat_id", columnNames = {"match_seat_id"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderSeat extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(name = "match_seat_id", nullable = false)
	private Long matchSeatId;

	@Column(name = "block_id", nullable = false)
	private Long blockId;

	@Column(name = "section_id", nullable = false)
	private Long sectionId;

	@Column(name = "row_no", nullable = false)
	private Integer rowNo;

	@Column(name = "seat_no", nullable = false)
	private Integer seatNo;

	@Column(name = "price", nullable = false)
	private Integer price;

	@Enumerated(EnumType.STRING)
	@Column(name = "ticket_type", nullable = false, length = 30)
	private TicketType ticketType;

	@Builder
	public OrderSeat(
		Order order,
		Long matchSeatId,
		Long blockId,
		Long sectionId,
		Integer rowNo,
		Integer seatNo,
		Integer price,
		TicketType ticketType
	) {
		this.order = order;
		this.matchSeatId = matchSeatId;
		this.blockId = blockId;
		this.sectionId = sectionId;
		this.rowNo = rowNo;
		this.seatNo = seatNo;
		this.price = price;
		this.ticketType = ticketType;
	}
}
