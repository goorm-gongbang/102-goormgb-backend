package com.goormgb.be.seat.pricePolicy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.domain.ticket.enums.TicketType;
import com.goormgb.be.seat.pricePolicy.entity.PricePolicy;
import com.goormgb.be.seat.pricePolicy.enums.DayType;

public interface PricePolicyRepository extends JpaRepository<PricePolicy, Long> {

	Optional<PricePolicy> findBySectionIdAndDayTypeAndTicketType(Long sectionId, DayType dayType, TicketType ticketType);
}
