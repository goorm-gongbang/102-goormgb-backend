package com.goormgb.be.ordercore.fixture.club;

import com.goormgb.be.ordercore.club.dto.response.ClubDetailGetResponse;
import com.goormgb.be.ordercore.club.dto.response.ClubGetResponse;
import com.goormgb.be.ordercore.match.dto.response.ClubMonthlyMatchesResponse;
import com.goormgb.be.ordercore.match.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        return new ClubDetailGetResponse(
                clubId,
                "구름 FC",
                "https://cdn.goormgb.com/logo/goorm.png",
                "#0055FF",
                new ClubDetailGetResponse.StadiumDto(
                        10L,
                        "구름 스타디움"
                ),
                "https://www.goormfc.com",
                new ClubDetailGetResponse.CurrentSeasonStatsDto(
                        2026,
                        1,
                        18,
                        3,
                        5,
                        new BigDecimal("0.720"),
                        new BigDecimal("0.312"),
                        new BigDecimal("2.85"),
                        new BigDecimal("0.0")
                )
        );
    }

    public static ClubDetailGetResponse clubDetailGetResponseWithoutStats(Long clubId) {
        return new ClubDetailGetResponse(
                clubId,
                "구름 FC",
                "https://cdn.goormgb.com/logo/goorm.png",
                "#0055FF",
                new ClubDetailGetResponse.StadiumDto(
                        10L,
                        "구름 스타디움"
                ),
                "https://www.goormfc.com",
                null
        );
    }

    public static ClubMonthlyMatchesResponse clubMonthlyMatchesResponse(Long clubId, int year, int month) {
        return ClubMonthlyMatchesResponse.of(
                clubId,
                year,
                month,
                List.of(
                        new ClubMonthlyMatchesResponse.MatchItem(
                                1001L,
                                LocalDateTime.of(year, month, 10, 18, 30),
                                new ClubMonthlyMatchesResponse.OpponentClub(
                                        2L,
                                        "테크업 유나이티드",
                                        "https://cdn.goormgb.com/logo/techup.png"
                                ),
                                SaleStatus.ON_SALE,
                                true   // 홈 경기
                        ),
                        new ClubMonthlyMatchesResponse.MatchItem(
                                1002L,
                                LocalDateTime.of(year, month, 24, 19, 0),
                                new ClubMonthlyMatchesResponse.OpponentClub(
                                        3L,
                                        "코딩 시티",
                                        "https://cdn.goormgb.com/logo/coding.png"
                                ),
                                SaleStatus.SOLD_OUT,
                                false  // 원정 경기
                        )
                )
        );
    }
}
