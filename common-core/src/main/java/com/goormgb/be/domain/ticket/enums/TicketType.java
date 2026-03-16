package com.goormgb.be.domain.ticket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketType {

	ADULT("일반"),
	YOUTH("청소년"),
	MILITARY("군경"),
	CHILD("어린이"),
	SENIOR("경로"),
	VETERAN("국가유공자"),
	DISABLED("장애인");

	private final String description;
}
