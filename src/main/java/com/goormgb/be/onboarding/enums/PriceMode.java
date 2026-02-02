package com.goormgb.be.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PriceMode {
    ANY("무관"),
    RANGE("범위 지정");

    private final String description;
}
