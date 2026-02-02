package com.goormgb.be.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ObstructionSensitivity {
    NET_SENSITIVE("안전망 민감"),
    RAIL_PILLAR_SENSITIVE("난간/기둥 민감"),
    NORMAL("보통"),
    ANY("둔감/ 무관");

    private final String description;
}
