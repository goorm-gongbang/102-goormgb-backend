package com.goormgb.be.seat.pricePolicy.entity;

import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.seat.pricePolicy.enums.DayType;
import com.goormgb.be.seat.pricePolicy.enums.TicketType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "price_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PricePolicy extends BaseEntity {

	@Column(name = "stadium_id", nullable = false)
	private Long stadiumId;

	@Column(name = "section_id", nullable = false)
	private Long sectionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "day_type", nullable = false, length = 20)
	private DayType dayType;

	@Enumerated(EnumType.STRING)
	@Column(name = "ticket_type", nullable = false, length = 30)
	private TicketType ticketType;

	@Column(name = "price", nullable = false)
	private Integer price;

	@Builder
	public PricePolicy(
		Long stadiumId,
		Long sectionId,
		DayType dayType,
		TicketType ticketType,
		Integer price
	) {
		this.stadiumId = stadiumId;
		this.sectionId = sectionId;
		this.dayType = dayType;
		this.ticketType = ticketType;
		this.price = price;
	}

	public void updatePrice(Integer price) {
		this.price = price;
	}
}
