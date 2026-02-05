package com.goormgb.be.auth.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
// TODO: JWT 토큰 타입 이넘 설정
@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("ACCESS"),
    REFRESH("REFRESH");

    private final String value;
}