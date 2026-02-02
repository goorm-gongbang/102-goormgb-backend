package com.goormgb.be.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatPositionPref {
    AISLE("통로석 선호"),
    MIDDLE("중앙석 선호"),
    ANY("무관");

    private final String description;
}
