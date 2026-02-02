package com.goormgb.be.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialProvider {
    KAKAO("카카오");

    private final String description;
}
