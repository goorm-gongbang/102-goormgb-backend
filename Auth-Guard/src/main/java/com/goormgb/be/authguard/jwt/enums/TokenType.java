package com.goormgb.be.authguard.jwt.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("ACCESS"),
    REFRESH("REFRESH");

    private final String value;
}
