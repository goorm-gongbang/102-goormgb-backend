package com.goormgb.be.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WithdrawStatus {
    REQUESTED("탈퇴 요청");
    // CANCELLED("요청 취소"); // 우리 서비스에서 탈퇴 취소 요청은 없음

    private final String description;
}
