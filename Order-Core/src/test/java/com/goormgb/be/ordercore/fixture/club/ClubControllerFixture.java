package com.goormgb.be.ordercore.fixture.club;

import com.goormgb.be.ordercore.club.dto.response.ClubDetailGetResponse;
import com.goormgb.be.ordercore.club.dto.response.ClubGetResponse;
import com.goormgb.be.ordercore.match.dto.response.ClubMonthlyMatchesResponse;

import java.util.List;

public final class ClubControllerFixture {

    private ClubControllerFixture() {}

    public static Long clubId() {
        return 1L;
    }

    public static int year() {
        return 2026;
    }

    public static int month() {
        return 3;
    }

    public static ClubGetResponse clubGetResponse() {
        return new ClubGetResponse(
                List.of(
                        new ClubGetResponse.ClubItem(
                                1L,
                                "구름 FC",
                                "GOORM FC",
                                "https://cdn.goormgb.com/logo/goorm.png",
                                "#0055FF"
                        ),
                        new ClubGetResponse.ClubItem(
                                2L,
                                "테크업 유나이티드",
                                "TECHUP UNITED",
                                "https://cdn.goormgb.com/logo/techup.png",
                                "#FF3300"
                        ),
                        new ClubGetResponse.ClubItem(
                                3L,
                                "코딩 시티",
                                "CODING CITY",
                                "https://cdn.goormgb.com/logo/coding.png",
                                "#00AA66"
                        )
                )
        );
    }

    public static ClubDetailGetResponse clubDetailGetResponse(Long clubId) {
        // TODO: 실제 DTO 필드에 맞춰 생성 로직 작성
        return null;
    }

    public static ClubMonthlyMatchesResponse clubMonthlyMatchesResponse(Long clubId, int year, int month) {
        // TODO: 실제 DTO 필드에 맞춰 생성 로직 작성
        return null;
    }
}
